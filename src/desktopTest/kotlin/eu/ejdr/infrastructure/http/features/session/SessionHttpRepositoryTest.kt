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
    fun `createLobby success maps the lobby and participants`() = runTest {
        val body =
            """{"sessionId":"s-1","status":"LOBBY","participants":[{"userId":"u-player","status":"INVITED","characterSheetId":null}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body))
            .createLobby("s-1", listOf("u-player"))

        assertIs<Result.Success<eu.ejdr.domain.features.session.entities.SessionLobby>>(result)
        assertEquals("LOBBY", result.value.status)
        assertEquals(1, result.value.participants.size)
        assertEquals("u-player", result.value.participants.first().userId)
        assertEquals("INVITED", result.value.participants.first().status)
    }

    @Test
    fun `createLobby 409 SESSION_NOT_LAUNCHABLE maps to SessionNotLaunchable`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"SESSION_NOT_LAUNCHABLE"}"""),
        ).createLobby("s-1", listOf("u-player"))

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.SessionNotLaunchable, result.error)
    }

    @Test
    fun `createLobby 400 EMPTY_PARTICIPANT_SELECTION maps to EmptyParticipantSelection`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"EMPTY_PARTICIPANT_SELECTION"}"""),
        ).createLobby("s-1", emptyList())

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.EmptyParticipantSelection, result.error)
    }

    @Test
    fun `inviteToLobby success maps the refreshed lobby`() = runTest {
        val body =
            """{"sessionId":"s-1","status":"LOBBY","participants":[{"userId":"u-present","status":"ACCEPTED","characterSheetId":null},{"userId":"u-refus","status":"INVITED","characterSheetId":null}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body))
            .inviteToLobby("s-1", listOf("u-refus"))

        assertIs<Result.Success<eu.ejdr.domain.features.session.entities.SessionLobby>>(result)
        assertEquals("LOBBY", result.value.status)
        // Le lobby renvoyé est complet : le joueur reconvié y est repassé « en attente ».
        assertEquals(2, result.value.participants.size)
        assertEquals("INVITED", result.value.participants.last().status)
    }

    @Test
    fun `inviteToLobby 409 LOBBY_NOT_OPEN maps to LobbyNotOpen`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"LOBBY_NOT_OPEN"}"""),
        ).inviteToLobby("s-1", listOf("u-player"))

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.LobbyNotOpen, result.error)
    }

    @Test
    fun `removeParticipant success maps the lobby without the removed player`() = runTest {
        val body =
            """{"sessionId":"s-1","status":"LOBBY","participants":[{"userId":"u-present","status":"ACCEPTED","characterSheetId":null}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body))
            .removeParticipant("s-1", "u-retire")

        assertIs<Result.Success<eu.ejdr.domain.features.session.entities.SessionLobby>>(result)
        assertEquals("LOBBY", result.value.status)
        // Le serveur renvoie le lobby complet à jour : le joueur retiré n'y figure plus.
        assertEquals(1, result.value.participants.size)
        assertEquals("u-present", result.value.participants.single().userId)
    }

    @Test
    fun `removeParticipant 409 LOBBY_NOT_OPEN maps to LobbyNotOpen`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"LOBBY_NOT_OPEN"}"""),
        ).removeParticipant("s-1", "u-player")

        assertIs<Result.Failure<SessionError>>(result)
        assertEquals(SessionError.LobbyNotOpen, result.error)
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
