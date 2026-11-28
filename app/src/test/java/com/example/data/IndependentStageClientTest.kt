package com.example.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class IndependentStageClientTest {
    private val token = "a".repeat(43)
    private val id = "10000000-0000-4000-8000-000000000001"
    @Test fun disabledNeverSends() = runBlocking {
        var calls = 0
        val client = IndependentStageClient(false, StageTransport { calls++;StageResponse(200,JsonObject(emptyMap())) })
        assertTrue(runCatching { client.setSession(token) }.isFailure)
        assertTrue(runCatching { client.list() }.isFailure)
        assertEquals(0,calls)
    }
    @Test fun validatesSyntheticAndClearsUnauthorized() = runBlocking {
        var calls = 0
        val client = IndependentStageClient(true,StageTransport {
            calls++
            if(calls == 1) StageResponse(200,buildJsonObject {
                put("profiles",buildJsonArray { add(buildJsonObject {
                    put("id",id);put("alias","demo-child");put("age_months",24)
                }) })
            }) else StageResponse(401,JsonObject(emptyMap()))
        })
        client.setSession(token)
        assertEquals(listOf(StageProfile(id,"demo-child",24)),client.list())
        assertTrue(runCatching { client.create("real name",24) }.isFailure)
        assertTrue(runCatching { client.grant(id,id,false) }.isFailure)
        assertEquals(1,calls)
        assertTrue(runCatching { client.list() }.isFailure)
        assertTrue(runCatching { client.list() }.isFailure)
        assertEquals(2,calls)
    }
    @Test fun previousAccountResponseIsRejected() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<StageResponse>()
        val client = IndependentStageClient(true, StageTransport {
            started.complete(Unit)
            response.await()
        })
        client.setSession(token)
        val previous = async { runCatching { client.list() } }
        started.await()
        client.setSession("b".repeat(43))
        response.complete(StageResponse(200,buildJsonObject { put("profiles",JsonArray(emptyList())) }))
        assertTrue(previous.await().isFailure)
    }
    @Test fun logoutAndMalformedResponseFailClosed() = runBlocking {
        val client = IndependentStageClient(true, StageTransport {
            if(it.path.endsWith("/logout")) StageResponse(200,buildJsonObject { put("ok",true) })
            else StageResponse(200,buildJsonObject { put("profiles",buildJsonArray {
                add(buildJsonObject { put("id",id);put("alias","real child");put("age_months",24) })
            }) })
        })
        client.setSession(token)
        assertTrue(runCatching { client.list() }.isFailure)
        client.logout()
        assertTrue(runCatching { client.list() }.isFailure)
    }
    @Test fun careCardContractRejectsUnconfirmedIssuanceAndUnexpectedState() = runBlocking {
        val calls = mutableListOf<StageRequest>()
        val client = IndependentStageClient(true, StageTransport { req ->
            calls.add(req)
            if(req.method == "GET") StageResponse(200,buildJsonObject {
                put("cards",buildJsonArray { add(buildJsonObject {
                    put("id",id);put("pharmacy_id",id);put("template","demo-care-v1");put("state","issued")
                }) })
            }) else if(req.path.endsWith("/state")) StageResponse(200,buildJsonObject { put("ok",true) })
            else StageResponse(201,buildJsonObject { put("id",id) })
        })
        client.setSession(token)
        assertEquals(listOf(StageCareCard(id,id,CareState.ISSUED)),client.listCare(id))
        assertTrue(runCatching { client.issueCare(id,id,false) }.isFailure)
        assertEquals(1,calls.size)
        assertEquals(id,client.issueCare(id,id,true))
        client.transitionCare(id,id,CareState.ACKNOWLEDGED)
        assertEquals(JsonPrimitive("demo-care-v1"),calls[1].body?.get("template"))
        assertEquals(JsonPrimitive("acknowledged"),calls[2].body?.get("state"))
        assertTrue(runCatching { client.transitionCare(id,id,CareState.ISSUED) }.isFailure)
        assertEquals(3,calls.size)
    }
}
