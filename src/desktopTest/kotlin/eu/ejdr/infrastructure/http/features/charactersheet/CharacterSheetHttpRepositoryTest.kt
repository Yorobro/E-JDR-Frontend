package eu.ejdr.infrastructure.http.features.charactersheet

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.Purse
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.infrastructure.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharacterSheetHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-sheet-test").toFile()
    private val config = AppConfig(
        baseUrl = "http://localhost:3000",
        enableHttpLogging = false,
    )

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }
    }

    private fun repository(client: HttpClient) = CharacterSheetHttpRepository(client, config)

    @Test
    fun `list scopes the request by the active group`() = runTest {
        val body =
            """{"characterSheets":[{"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        var requestedUrl = ""
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }

        val result = repository(client).list("g-1")

        assertIs<Result.Success<List<CharacterSheet>>>(result)
        assertEquals("Aragorn", result.value.first().name)
        assertTrue(requestedUrl.contains("groupId=g-1"), "URL should carry groupId: $requestedUrl")
    }

    @Test
    fun `create sends name and group in the body`() = runTest {
        val body = """{"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}"""
        var requestBody = ""
        val engine = MockEngine { request ->
            requestBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }

        val result = repository(client).create("Aragorn", "g-1", "c-1")

        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("s-1", result.value.id)
        assertTrue(requestBody.contains("\"groupId\":\"g-1\""), "Body should carry groupId: $requestBody")
        assertTrue(
            requestBody.contains("\"campaignId\":\"c-1\""),
            "Body should carry campaignId: $requestBody",
        )
    }

    @Test
    fun `copy posts the target campaign and maps the new sheet`() = runTest {
        val body = """{"id":"s-2","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}"""
        var requestBody = ""
        var requestedUrl = ""
        val engine = MockEngine { request ->
            requestBody = (request.body as io.ktor.http.content.TextContent).text
            requestedUrl = request.url.toString()
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }

        val result = repository(client).copyToCampaign("s-1", "c-2")

        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("s-2", result.value.id)
        assertTrue(requestedUrl.contains("/character-sheets/s-1/copy"), "URL: $requestedUrl")
        assertTrue(
            requestBody.contains("\"targetCampaignId\":\"c-2\""),
            "Body should carry targetCampaignId: $requestBody",
        )
    }

    @Test
    fun `list response without detail fields still deserializes (defaults null)`() = runTest {
        // La liste est une projection nom-seul : les clés détaillées sont ABSENTES.
        // Les défauts du DTO doivent permettre la désérialisation (ignoreUnknownKeys ne couvre
        // que les clés EN TROP, pas les manquantes).
        val body =
            """{"characterSheets":[{"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).list("g-1")

        assertIs<Result.Success<List<CharacterSheet>>>(result)
        val sheet = result.value.first()
        assertEquals("Aragorn", sheet.name)
        assertNull(sheet.peupleId)
        assertNull(sheet.vigueur)
        assertNull(sheet.notes)
    }

    @Test
    fun `getById success maps the full sheet`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z",
             "peupleId":"peuple-1","vigueur":6,"notes":"Garde du Nord"}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getById("s-1")

        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("peuple-1", result.value.peupleId)
        assertEquals(6, result.value.vigueur)
        assertEquals("Garde du Nord", result.value.notes)
        assertNull(result.value.formationId)
    }

    @Test
    fun `getById maps sexe niveau formationId and purse`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z",
             "sexe":"M","niveau":5,"formationId":"formation-1",
             "purse":{"gold":1,"silver":50,"copper":0}}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getById("s-1")

        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("M", result.value.sexe)
        assertEquals(5, result.value.niveau)
        assertEquals("formation-1", result.value.formationId)
        assertEquals(Purse(gold = 1, silver = 50, copper = 0), result.value.purse)
    }

    @Test
    fun `getById maps the resolved formation and peuple blocks`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z",
             "social":2,
             "peuple":{"id":"peuple-1","name":"Dúnedain","stat":"social","bonus":1},
             "formation":{"id":"formation-1","name":"Rôdeur","stat":"social","bonus":2,
              "competences":[{"id":"comp-1","name":"Pistage"}]}}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getById("s-1")

        assertIs<Result.Success<CharacterSheet>>(result)
        val sheet = result.value
        assertEquals("Dúnedain", sheet.peuple?.name)
        assertEquals("social", sheet.peuple?.stat)
        assertEquals(1, sheet.peuple?.bonus)
        assertEquals("Rôdeur", sheet.formation?.name)
        assertEquals(2, sheet.formation?.bonus)
        assertEquals(listOf("Pistage"), sheet.formation?.competences?.map { it.name })
    }

    @Test
    fun `getById without resolved blocks leaves formation and peuple null`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getById("s-1")

        assertIs<Result.Success<CharacterSheet>>(result)
        assertNull(result.value.formation)
        assertNull(result.value.peuple)
    }

    @Test
    fun `getById 404 maps to NotFound`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.NotFound, """{"code":"CHARACTER_SHEET_NOT_FOUND"}"""),
        ).getById("ghost")
        assertIs<Result.Failure<CharacterSheetError>>(result)
        assertEquals(CharacterSheetError.NotFound, result.error)
    }

    @Test
    fun `update success maps the returned sheet`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Strider","createdAt":"2026-06-13T10:00:00.000Z",
             "vigueur":7}
        """.trimIndent()
        val sheet = CharacterSheet(
            id = "s-1",
            ownerId = "u-1",
            name = "Strider",
            createdAt = "2026-06-13T10:00:00.000Z",
            vigueur = 7,
        )
        val result = repository(clientReturning(HttpStatusCode.OK, body)).update(sheet)

        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("Strider", result.value.name)
        assertEquals(7, result.value.vigueur)
    }

    @Test
    fun `create 400 maps to InvalidName`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_CHARACTER_SHEET_NAME"}"""),
        ).create("", "g-1", "c-1")
        assertIs<Result.Failure<CharacterSheetError>>(result)
        assertEquals(CharacterSheetError.InvalidName, result.error)
    }

    @Test
    fun `copy 409 GM rule maps to GmCannotJoinOwnCampaign`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"GM_CANNOT_JOIN_OWN_CAMPAIGN"}"""),
        ).copyToCampaign("s-1", "c-1")
        assertIs<Result.Failure<CharacterSheetError>>(result)
        assertEquals(CharacterSheetError.GmCannotJoinOwnCampaign, result.error)
    }

    @Test
    fun `listPendingForCampaign success maps the characters`() = runTest {
        val body =
            """{"characters":[{"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).listPendingForCampaign("c-1")

        assertIs<Result.Success<List<CharacterSheet>>>(result)
        assertEquals("Aragorn", result.value.first().name)
    }

    @Test
    fun `acceptCharacter success on 204`() = runTest {
        val result =
            repository(clientReturning(HttpStatusCode.NoContent, "")).acceptCharacter("c-1", "s-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `refuseCharacter success on 204`() = runTest {
        val result =
            repository(clientReturning(HttpStatusCode.NoContent, "")).refuseCharacter("c-1", "s-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `getCampaignsForSheet success maps the campaigns`() = runTest {
        val body = """
            {"campaigns":[{"campaignId":"c-1","campaignName":"Donjon","gameMasterPseudo":"MJ",
             "linkStatus":"PENDING"}]}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getCampaignsForSheet("s-1")

        assertIs<Result.Success<List<SheetCampaign>>>(result)
        assertEquals(listOf(SheetCampaign("c-1", "Donjon", "MJ", "PENDING")), result.value)
    }

    @Test
    fun `exportSheetPdf success returns the pdf bytes`() = runTest {
        val pdf = byteArrayOf(37, 80, 68, 70, 45) // %PDF-
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(pdf),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val result = repository(client).exportSheetPdf("s-1")
        assertIs<Result.Success<ByteArray>>(result)
        assertContentEquals(pdf, result.value)
    }

    @Test
    fun `exportSheetPdf 403 maps to AccessDenied`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Forbidden, """{"code":"CHARACTER_SHEET_ACCESS_DENIED"}"""),
        ).exportSheetPdf("s-1")
        assertIs<Result.Failure<CharacterSheetError>>(result)
        assertEquals(CharacterSheetError.AccessDenied, result.error)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val result = repository(client).list("g-1")
        assertIs<Result.Failure<CharacterSheetError>>(result)
        assertEquals(CharacterSheetError.Network, result.error)
    }
}
