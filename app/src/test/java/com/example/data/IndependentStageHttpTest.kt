package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// JVM-only test transport. The operator gate never enters Android main sources or an APK.
class IndependentStageHttpTest {
    @Test fun realHttpAndPostgresLifecycle() = runBlocking {
        val required = System.getenv("MANO_KOTLIN_HTTP_REQUIRED") == "true"
        assumeTrue(required)
        val base = checkNotNull(System.getenv("MANO_KOTLIN_HTTP_BASE"))
        val gate = checkNotNull(System.getenv("MANO_KOTLIN_HTTP_GATE"))
        val uri = URI(base)
        check(uri.scheme == "http" && uri.host == "127.0.0.1" && uri.port > 0 && uri.path.isEmpty())
        val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build()
        val transport = StageTransport { r ->
            withContext(Dispatchers.IO) {
                val builder = HttpRequest.newBuilder(URI(base + r.path))
                    .timeout(Duration.ofSeconds(5))
                    .header("x-mano-operator-token", gate)
                    .header("Authorization", "Bearer " + r.bearer)
                if (r.body != null) builder.header("Content-Type", "application/json")
                val req = builder.method(r.method, r.body?.let { HttpRequest.BodyPublishers.ofString(it.toString()) }
                    ?: HttpRequest.BodyPublishers.noBody()).build()
                val response = http.send(req,HttpResponse.BodyHandlers.ofString())
                StageResponse(response.statusCode(),Json.parseToJsonElement(response.body()))
            }
        }
        val a = IndependentStageClient(true,transport)
        val b = IndependentStageClient(true,transport)
        val p = IndependentStageClient(true,transport)
        a.setSession(checkNotNull(System.getenv("MANO_KOTLIN_HTTP_PARENT")))
        b.setSession(checkNotNull(System.getenv("MANO_KOTLIN_HTTP_OTHER")))
        p.setSession(checkNotNull(System.getenv("MANO_KOTLIN_HTTP_PHARMACIST")))
        val pharmacy = checkNotNull(System.getenv("MANO_KOTLIN_HTTP_PHARMACY"))
        val id = a.create("demo-kotlin",24)
        assertTrue(a.list().any { it.id == id && it.ageMonths == 24 })
        assertFalse(b.list().any { it.id == id })
        assertFalse(p.list().any { it.id == id })
        assertTrue(runCatching { b.grant(id,pharmacy,true) }.isFailure)
        a.grant(id,pharmacy,true)
        assertTrue(p.list().any { it.id == id })
        val card = p.issueCare(id,pharmacy,true)
        assertTrue(a.listCare(id).any { it.id == card && it.state == CareState.ISSUED })
        assertTrue(runCatching { b.listCare(id) }.isFailure)
        a.transitionCare(id,card,CareState.ACKNOWLEDGED)
        a.transitionCare(id,card,CareState.FOLLOWUP_REQUESTED)
        assertTrue(p.listCare(id).any { it.id == card && it.state == CareState.FOLLOWUP_REQUESTED })
        p.transitionCare(id,card,CareState.CLOSED)
        assertTrue(a.listCare(id).any { it.id == card && it.state == CareState.CLOSED })
        a.revoke(id,pharmacy)
        assertTrue(runCatching { p.listCare(id) }.isFailure)
        assertFalse(p.list().any { it.id == id })
        a.logout(); b.logout(); p.logout()
        assertTrue(runCatching { a.list() }.isFailure)
        assertTrue(runCatching { p.list() }.isFailure)
    }
}
