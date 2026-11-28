package com.example.data

import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*

// Internal synthetic adapter only. The injected test transport holds server-only
// operator credentials outside the APK. No Firebase fallback or persistent token.
internal data class StageRequest(
    val method: String, val path: String, val bearer: String, val body: JsonObject? = null
) {
    override fun toString(): String = "StageRequest(redacted)"
}
internal data class StageResponse(val status: Int, val data: JsonElement)
internal fun interface StageTransport {
    suspend fun send(request: StageRequest): StageResponse
}
internal data class StageProfile(val id: String, val alias: String, val ageMonths: Int)
internal enum class CareState(val wire: String) {
    ISSUED("issued"), ACKNOWLEDGED("acknowledged"), FOLLOWUP_REQUESTED("followup_requested"), CLOSED("closed")
}
internal data class StageCareCard(val id: String, val pharmacyId: String, val state: CareState)
internal class IndependentStageClient(
    private val enabled: Boolean = false,
    private val transport: StageTransport
) {
    private val uuid = Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")
    private val aliasPattern = Regex("^demo-[a-z0-9-]{1,40}$")
    private val lock = Any()
    private var session: String? = null
    private var generation = 0L
    fun clearSession() = synchronized(lock) { generation++; session = null }
    fun setSession(token: String) = synchronized(lock) {
        generation++; session = null
        check(enabled && Regex("^[A-Za-z0-9_-]{43}$").matches(token)) { "STAGE_DISABLED_OR_INVALID_SESSION" }
        session = token
    }
    private suspend fun request(method: String, path: String, expected: Int, body: JsonObject? = null): JsonObject {
        val (token, epoch) = synchronized(lock) {
            check(enabled) { "STAGE_DISABLED" }
            Pair(checkNotNull(session) { "STAGE_UNAVAILABLE" }, generation)
        }
        val result = withTimeout(8_000L) { transport.send(StageRequest(method,path,token,body)) }
        return synchronized(lock) {
            check(generation == epoch) { "STALE_SESSION" }
            if (result.status == 401) { generation++; session = null }
            check(result.status == expected) { "STAGE_REQUEST_REJECTED" }
            result.data as? JsonObject ?: error("INVALID_RESPONSE")
        }
    }
    private fun str(obj: JsonObject, key: String): String {
        val p = obj[key] as? JsonPrimitive ?: error("INVALID_RESPONSE")
        check(p.isString) { "INVALID_RESPONSE" }
        return p.content
    }
    suspend fun list(): List<StageProfile> {
        val rows = request("GET","/internal/mano/v1/profiles",200)["profiles"] as? JsonArray ?: error("INVALID_RESPONSE")
        check(rows.size <= 100) { "INVALID_RESPONSE" }
        return rows.map {
            val p = it as? JsonObject ?: error("INVALID_RESPONSE")
            val id = str(p,"id")
            val alias = str(p,"alias")
            val ageValue = p["age_months"] as? JsonPrimitive ?: error("INVALID_RESPONSE")
            val age = ageValue.intOrNull ?: error("INVALID_RESPONSE")
            check(!ageValue.isString && uuid.matches(id) && aliasPattern.matches(alias) && age in 0..215) { "INVALID_RESPONSE" }
            StageProfile(id,alias,age)
        }
    }
    suspend fun create(alias: String, ageMonths: Int): String {
        require(aliasPattern.matches(alias) && ageMonths in 0..215) { "SYNTHETIC_ONLY" }
        val id = str(request("POST","/internal/mano/v1/profiles",201,buildJsonObject {
            put("alias",alias);put("ageMonths",ageMonths)
        }),"id")
        check(uuid.matches(id)) { "INVALID_RESPONSE" }
        return id
    }
    suspend fun grant(id: String, pharmacyId: String, confirmed: Boolean) {
        require(uuid.matches(id) && uuid.matches(pharmacyId) && confirmed) { "CONSENT_REQUIRED" }
        val r = request("POST","/internal/mano/v1/profiles/"+id+"/shares",200,buildJsonObject {
            put("pharmacyId",pharmacyId);put("consent","synthetic-v1")
        })
        check(r["ok"] == JsonPrimitive(true)) { "INVALID_RESPONSE" }
    }
    suspend fun revoke(id: String, pharmacyId: String) {
        require(uuid.matches(id) && uuid.matches(pharmacyId)) { "INVALID_ID" }
        val r = request("DELETE","/internal/mano/v1/profiles/"+id+"/shares/"+pharmacyId,200)
        check(r["ok"] == JsonPrimitive(true)) { "INVALID_RESPONSE" }
    }
    suspend fun listCare(profileId: String): List<StageCareCard> {
        require(uuid.matches(profileId))
        val rows = request("GET","/internal/mano/v1/profiles/"+profileId+"/care-cards",200)["cards"] as? JsonArray ?: error("INVALID_RESPONSE")
        check(rows.size <= 100)
        return rows.map {
            val row = it as? JsonObject ?: error("INVALID_RESPONSE")
            val id = str(row,"id")
            val pharmacy = str(row,"pharmacy_id")
            check(uuid.matches(id) && uuid.matches(pharmacy) && str(row,"template")=="demo-care-v1")
            val state = CareState.entries.firstOrNull { state -> state.wire == str(row,"state") } ?: error("INVALID_RESPONSE")
            StageCareCard(id,pharmacy,state)
        }
    }
    suspend fun issueCare(profileId: String, pharmacyId: String, confirmed: Boolean): String {
        require(uuid.matches(profileId) && uuid.matches(pharmacyId) && confirmed)
        val id = str(request("POST","/internal/mano/v1/profiles/"+profileId+"/care-cards",201,buildJsonObject {
            put("pharmacyId",pharmacyId);put("template","demo-care-v1")
        }),"id")
        check(uuid.matches(id))
        return id
    }
    suspend fun transitionCare(profileId: String, cardId: String, state: CareState) {
        require(uuid.matches(profileId) && uuid.matches(cardId) && state != CareState.ISSUED)
        val r = request("POST","/internal/mano/v1/profiles/"+profileId+"/care-cards/"+cardId+"/state",200,buildJsonObject { put("state",state.wire) })
        check(r["ok"] == JsonPrimitive(true))
    }
    suspend fun logout() {
        val epoch = synchronized(lock) { generation }
        try {
            check(request("POST","/internal/mano/v1/session/logout",200)["ok"] == JsonPrimitive(true)) { "INVALID_RESPONSE" }
        } finally {
            synchronized(lock) { if (generation == epoch) { generation++;session = null } }
        }
    }
}
