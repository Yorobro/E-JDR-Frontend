package eu.ejdr.infrastructure.realtime.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class InvalidationDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `désérialise un event invalidate du backend`() {
        val raw =
            """{"type":"invalidate","channel":"user:u1","resource":"character-sheets","scopeId":"u1"}"""
        val dto = json.decodeFromString(InvalidationDto.serializer(), raw)
        assertEquals("invalidate", dto.type)
        assertEquals("character-sheets", dto.resource)
        assertEquals("u1", dto.scopeId)
    }
}
