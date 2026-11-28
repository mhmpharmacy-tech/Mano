package com.example.data

import kotlinx.coroutines.runBlocking
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
}
