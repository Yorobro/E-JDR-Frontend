package eu.ejdr.infrastructure.http.features.reference

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

/**
 * Tests de l'adapter HTTP [ReferenceHttpRepository] avec le MockEngine de Ktor. Couvre la
 * désérialisation des réponses/erreurs, la traduction des codes en [ReferenceError] et le réseau.
 */
class ReferenceHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-reference-test").toFile()
    private val config = AppConfig(
        baseUrl = "http://localhost:3000",
        dataDir = tmpDir,
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
    fun `list success maps the wrapped items`() = runTest {
        val body =
            """{"items":[{"id":"a-1","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).list(ReferenceType.ARME)

        assertIs<Result.Success<List<ReferenceItem>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("Épée", result.value.first().name)
    }

    @Test
    fun `create success maps the item`() = runTest {
        val body = """{"id":"a-1","name":"Épée","createdAt":"2026-06-13T10:00:00.000Z"}"""
        val result = repository(clientReturning(HttpStatusCode.Created, body))
            .create(ReferenceType.ARME, "Épée")

        assertIs<Result.Success<ReferenceItem>>(result)
        assertEquals("a-1", result.value.id)
    }

    @Test
    fun `create 409 maps to NameAlreadyUsed`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"REFERENCE_NAME_ALREADY_USED"}"""),
        ).create(ReferenceType.ARME, "Épée")

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.NameAlreadyUsed, result.error)
    }

    @Test
    fun `create 400 maps to InvalidName`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_REFERENCE_NAME"}"""),
        ).create(ReferenceType.ARME, "")

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

        val result = repository(client).list(ReferenceType.ARME)

        assertIs<Result.Failure<ReferenceError>>(result)
        assertEquals(ReferenceError.Network, result.error)
    }
}
