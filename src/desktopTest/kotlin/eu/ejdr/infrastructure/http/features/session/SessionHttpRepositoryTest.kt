package eu.ejdr.infrastructure.http.features.session

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError
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
 * Tests de l'adapter HTTP [SessionHttpRepository] avec le MockEngine de Ktor.
 *
 * Couvre la sérialisation des requêtes, la désérialisation des réponses/erreurs, la traduction
 * des statuts/codes en [SessionError] et la gestion des échecs réseau.
 */
class SessionHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-session-test").toFile()
    private val config = AppConfig(
        baseUrl = "http://localhost:3000",
        dataDir = tmpDir.absolutePath,
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

    private fun repository(client: HttpClient) = SessionHttpRepository(client, config)

    @Test
    fun `listByCampaign success maps the wrapped sessions`() = runTest {
        val body =
            """{"sessions":[{"id":"s-1","campaignId":"c-1","title":"Intro","date":"2026-06-20","createdAt":"2026-06-13T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).listByCampaign("c-1")

        assertIs<Result.Success<List<Session>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("Intro", result.value.first().title)
        assertEquals("2026-06-20", result.value.first().date)
    }

    @Test
    fun `create success maps the session`() = runTest {
        val body =
            """{"id":"s-1","campaignId":"c-1","title":"Intro","date":"2026-06-20","createdAt":"2026-06-13T10:00:00.000Z"}"""
        val result = repository(clientReturning(HttpStatusCode.Created, body))
            .create("c-1", "Intro", "2026-06-20")

        assertIs<Result.Success<Session>>(result)
        assertEquals("s-1", result.value.id)
        assertEquals("c-1", result.value.campaignId)
    }

    @Test
    fun `create 400 INVALID_SESSION_DATE maps to InvalidDate`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_SESSION_DATE"}"""),
        ).create("c-1", "Intro", "bad")

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.InvalidDate, result.error)
    }

    @Test
    fun `get success maps the session`() = runTest {
        val body =
            """{"id":"s-1","campaignId":"c-1","title":"Intro","date":"2026-06-20","createdAt":"2026-06-13T10:00:00.000Z"}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).get("s-1")

        assertIs<Result.Success<Session>>(result)
        assertEquals("Intro", result.value.title)
    }

    @Test
    fun `get 404 maps to NotFound`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.NotFound, """{"code":"SESSION_NOT_FOUND"}"""),
        ).get("ghost")

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.NotFound, result.error)
    }

    @Test
    fun `update success maps the session`() = runTest {
        val body =
            """{"id":"s-1","campaignId":"c-1","title":"Après","date":"2026-07-01","createdAt":"2026-06-13T10:00:00.000Z"}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body))
            .update("s-1", "Après", "2026-07-01")

        assertIs<Result.Success<Session>>(result)
        assertEquals("Après", result.value.title)
        assertEquals("2026-07-01", result.value.date)
    }

    @Test
    fun `update 403 maps to AccessDenied`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Forbidden, """{"code":"CAMPAIGN_ACCESS_DENIED"}"""),
        ).update("s-1", "X", "2026-07-01")

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.AccessDenied, result.error)
    }

    @Test
    fun `delete success on 204`() = runTest {
        val result = repository(clientReturning(HttpStatusCode.NoContent, "")).delete("s-1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = repository(client).listByCampaign("c-1")

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.Network, result.error)
    }
}
