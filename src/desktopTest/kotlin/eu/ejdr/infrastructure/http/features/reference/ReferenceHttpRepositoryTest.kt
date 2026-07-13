package eu.ejdr.infrastructure.http.features.reference

import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import eu.ejdr.application.features.reference.abstraction.ReferenceItemForm
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de l'adapter HTTP [ReferenceHttpRepository] avec le MockEngine de Ktor. Couvre la
 * désérialisation des réponses/erreurs, la traduction des codes en [ReferenceError] et le réseau.
 */
class ReferenceHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-reference-test").toFile()
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
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun repository(client: HttpClient) = ReferenceHttpRepository(client, config)

    @Test
    fun `list scopes the request by the active group`() = runTest {
        val body =
            """{"items":[{"id":"a-1","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
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

        val result = repository(client).list(ReferenceType.ARME, "g-1")

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        assertEquals("Épée", result.value.first().name)
        assertTrue(requestedUrl.contains("armes"), "URL should target the type slug: $requestedUrl")
        assertTrue(requestedUrl.contains("groupId=g-1"), "URL should carry groupId: $requestedUrl")
    }

    @Test
    fun `create sends name and group in the body`() = runTest {
        val body = """{"id":"a-1","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}"""
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

        val result = repository(client).create(ReferenceType.ARME, "g-1", ReferenceItemForm("Épée"))

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals("a-1", result.value.id)
        assertTrue(requestBody.contains("\"groupId\":\"g-1\""), "Body should carry groupId: $requestBody")
    }

    @Test
    fun `create sends stat bonus and competence ids in the body`() = runTest {
        val body =
            """{"id":"f-1","name":"Rôdeur","createdAt":"2026-06-13T10:00:00.000Z","stat":"dexterite","bonus":3,"competenceIds":["c-1","c-2"]}"""
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

        val result = repository(client).create(
            ReferenceType.FORMATION,
            "g-1",
            ReferenceItemForm(
                name = "Rôdeur",
                stat = "dexterite",
                bonus = 3,
                competenceIds = listOf("c-1", "c-2"),
            ),
        )

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals("dexterite", result.value.stat)
        assertEquals(3, result.value.bonus)
        assertEquals(listOf("c-1", "c-2"), result.value.competenceIds)
        assertTrue(requestBody.contains("\"stat\":\"dexterite\""), "Body should carry stat: $requestBody")
        assertTrue(requestBody.contains("\"bonus\":3"), "Body should carry bonus: $requestBody")
        assertTrue(
            requestBody.contains("\"competenceIds\":[\"c-1\",\"c-2\"]"),
            "Body should carry competenceIds: $requestBody",
        )
        // Contrat asymétrique : une formation n'envoie JAMAIS statBonuses.
        assertTrue(
            !requestBody.contains("statBonuses"),
            "A formation must not send statBonuses: $requestBody",
        )
    }

    @Test
    fun `create peuple sends the list of stat bonuses in the body`() = runTest {
        val body = """{"id":"p-1","name":"Nain","createdAt":"2026-06-13T10:00:00.000Z",""" +
            """"statBonuses":[{"stat":"vigueur","bonus":2},{"stat":"social","bonus":1}]}"""
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

        val result = repository(client).create(
            ReferenceType.PEUPLE,
            "g-1",
            ReferenceItemForm(
                name = "Nain",
                statBonuses = listOf(
                    ReferenceStatBonus("vigueur", 2),
                    ReferenceStatBonus("social", 1),
                ),
            ),
        )

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals(
            listOf(ReferenceStatBonus("vigueur", 2), ReferenceStatBonus("social", 1)),
            result.value.statBonuses,
        )
        assertTrue(
            requestBody.contains(
                """"statBonuses":[{"stat":"vigueur","bonus":2},{"stat":"social","bonus":1}]""",
            ),
            "Body should carry statBonuses: $requestBody",
        )
    }

    @Test
    fun `create armure sends protection points in the body`() = runTest {
        val body =
            """{"id":"a-1","name":"Cotte","createdAt":"2026-06-13T10:00:00.000Z","protectionPoints":5}"""
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

        val result = repository(client).create(
            ReferenceType.ARMURE,
            "g-1",
            ReferenceItemForm(
                name = "Cotte",
                protectionPoints = 5,
            ),
        )

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals(5, result.value.protectionPoints)
        assertTrue(
            requestBody.contains("\"protectionPoints\":5"),
            "Body should carry protectionPoints: $requestBody",
        )
    }

    @Test
    fun `create sort sends description in the body`() = runTest {
        val body =
            """{"id":"s-1","name":"Boule de feu","createdAt":"2026-06-13T10:00:00.000Z","description":"3d6 dégâts de feu."}"""
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

        val result = repository(client).create(
            ReferenceType.SORT,
            "g-1",
            ReferenceItemForm(
                name = "Boule de feu",
                description = "3d6 dégâts de feu.",
            ),
        )

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals("3d6 dégâts de feu.", result.value.description)
        assertTrue(
            requestBody.contains("\"description\":\"3d6 dégâts de feu.\""),
            "Body should carry description: $requestBody",
        )
    }

    @Test
    fun `list deserializes description tolerantly`() = runTest {
        val body = """{"items":[
            {"id":"s-1","name":"Boule de feu","createdAt":"2026-06-13T10:00:00.000Z","description":"3d6 feu"},
            {"id":"s-2","name":"Lumière","createdAt":"2026-06-13T10:00:00.000Z"}
        ]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).list(ReferenceType.SORT, "g-1")

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        assertEquals("3d6 feu", result.value.first().description)
        assertNull(result.value[1].description)
    }

    @Test
    fun `update sends a PUT to the item url with the full body`() = runTest {
        val body =
            """{"id":"f-1","name":"Rôdeur+","createdAt":"2026-06-13T10:00:00.000Z","stat":"vigueur","bonus":4,"competenceIds":["c-1"]}"""
        var requestMethod = ""
        var requestUrl = ""
        var requestBody = ""
        val engine = MockEngine { request ->
            requestMethod = request.method.value
            requestUrl = request.url.toString()
            requestBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }

        val result = repository(client).update(
            ReferenceType.FORMATION,
            "f-1",
            "g-1",
            ReferenceItemForm(
                name = "Rôdeur+",
                stat = "vigueur",
                bonus = 4,
                competenceIds = listOf("c-1"),
            ),
        )

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals("Rôdeur+", result.value.name)
        assertEquals("vigueur", result.value.stat)
        assertEquals("PUT", requestMethod)
        assertTrue(requestUrl.contains("formations/f-1"), "URL should target the item: $requestUrl")
        assertTrue(requestBody.contains("\"groupId\":\"g-1\""), "Body should carry groupId: $requestBody")
        assertTrue(requestBody.contains("\"stat\":\"vigueur\""), "Body should carry stat: $requestBody")
        assertTrue(requestBody.contains("\"competenceIds\":[\"c-1\"]"), "Body should carry ids: $requestBody")
    }

    @Test
    fun `update 409 maps to NameAlreadyUsed`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"REFERENCE_NAME_ALREADY_USED"}"""),
        ).update(ReferenceType.ARME, "a-1", "g-1", ReferenceItemForm("Épée"))

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.NameAlreadyUsed, result.error)
    }

    @Test
    fun `list deserializes protection points tolerantly`() = runTest {
        val body = """{"items":[
            {"id":"a-1","name":"Cotte","createdAt":"2026-06-13T10:00:00.000Z","protectionPoints":3},
            {"id":"a-2","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}
        ]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).list(ReferenceType.ARMURE, "g-1")

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        assertEquals(3, result.value.first().protectionPoints)
        assertNull(result.value[1].protectionPoints)
    }

    @Test
    fun `list deserializes stat bonus and competence ids tolerantly`() = runTest {
        val body = """{"items":[
            {"id":"f-1","name":"Rôdeur","createdAt":"2026-06-13T10:00:00.000Z","stat":"vigueur","bonus":2,"competenceIds":["c-1"]},
            {"id":"a-1","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}
        ]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).list(ReferenceType.FORMATION, "g-1")

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        val formation = result.value.first()
        assertEquals("vigueur", formation.stat)
        assertEquals(2, formation.bonus)
        assertEquals(listOf("c-1"), formation.competenceIds)
        val weapon = result.value[1]
        assertEquals(null, weapon.stat)
        assertEquals(null, weapon.bonus)
        assertTrue(weapon.competenceIds.isEmpty())
    }

    @Test
    fun `create 409 maps to NameAlreadyUsed`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"REFERENCE_NAME_ALREADY_USED"}"""),
        ).create(ReferenceType.ARME, "g-1", ReferenceItemForm("Épée"))

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.NameAlreadyUsed, result.error)
    }

    @Test
    fun `create 400 maps to InvalidName`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_REFERENCE_NAME"}"""),
        ).create(ReferenceType.ARME, "g-1", ReferenceItemForm(""))

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.InvalidName, result.error)
    }

    @Test
    fun `delete success on 204`() = runTest {
        val result =
            repository(clientReturning(HttpStatusCode.NoContent, "")).delete(ReferenceType.ARME, "a-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `delete 404 maps to NotFound`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.NotFound, """{"code":"REFERENCE_ITEM_NOT_FOUND"}"""),
        ).delete(ReferenceType.ARME, "ghost")

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.NotFound, result.error)
    }

    @Test
    fun `listLinked success maps the wrapped items`() = runTest {
        val body =
            """{"items":[{"id":"a-1","name":"Andúril","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        val result =
            repository(clientReturning(HttpStatusCode.OK, body)).listLinked("s-1", ReferenceType.ARME)

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        assertEquals("Andúril", result.value.first().name)
    }

    @Test
    fun `link success on 201`() = runTest {
        val result = repository(clientReturning(HttpStatusCode.Created, ""))
            .link("s-1", ReferenceType.ARME, "a-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `link 403 maps to AccessDenied`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Forbidden, """{"code":"CHARACTER_SHEET_ACCESS_DENIED"}"""),
        ).link("s-1", ReferenceType.ARME, "a-1")

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.AccessDenied, result.error)
    }

    @Test
    fun `unlink success on 204`() = runTest {
        val result = repository(clientReturning(HttpStatusCode.NoContent, ""))
            .unlink("s-1", ReferenceType.ARME, "a-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = repository(client).list(ReferenceType.ARME, "g-1")

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.Network, result.error)
    }
}
