package eu.ejdr.infrastructure.http.features.friendgroup

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError
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
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class FriendGroupHttpRepositoryTest {

    private val tmpDir = Files.createTempDirectory("ejdr-friendgroup-test").toFile()
    private val config = AppConfig(baseUrl = "http://localhost:3000", enableHttpLogging = false)

    @AfterTest fun cleanup() { tmpDir.deleteRecursively() }

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

    private fun repository(client: HttpClient) = FriendGroupHttpRepository(client, config)

    @Test
    fun `listMine success maps wrapped groups`() = runTest {
        val body = """{"groups":[{"id":"g1","name":"Tavernier","myRole":"ADMIN","createdAt":"2026-06-18T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).listMine()

        assertIs<Result.Success<List<FriendGroup>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("Tavernier", result.value.first().name)
        assertEquals("ADMIN", result.value.first().myRole)
    }

    @Test
    fun `create success maps the group`() = runTest {
        val body = """{"id":"g1","name":"Tavernier","myRole":"ADMIN","createdAt":"2026-06-18T10:00:00.000Z"}"""
        val result = repository(clientReturning(HttpStatusCode.Created, body)).create("Tavernier")

        assertIs<Result.Success<FriendGroup>>(result)
        assertEquals("g1", result.value.id)
    }

    @Test
    fun `create 400 maps to InvalidGroupName`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_GROUP_NAME"}"""),
        ).create("")

        assertIs<Result.Failure<FriendGroupError>>(result)
        assertEquals(FriendGroupError.InvalidGroupName, result.error)
    }

    @Test
    fun `delete 204 is success`() = runTest {
        val result = repository(clientReturning(HttpStatusCode.NoContent, "")).delete("g1")
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `delete 403 maps to NotMember`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Forbidden, """{"code":"NOT_GROUP_MEMBER"}"""),
        ).delete("g1")

        assertIs<Result.Failure<FriendGroupError>>(result)
        assertEquals(FriendGroupError.NotMember, result.error)
    }

    @Test
    fun `listMyInvitations success maps wrapped invitations`() = runTest {
        val body = """{"invitations":[{"id":"inv1","groupId":"g1","groupName":"Tavernier","invitedBy":"user-a","createdAt":"2026-06-18T10:00:00.000Z"}]}"""
        val result = repository(clientReturning(HttpStatusCode.OK, body)).listMyInvitations()

        assertIs<Result.Success<List<GroupInvitation>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("Tavernier", result.value.first().groupName)
    }

    @Test
    fun `acceptInvitation 409 maps to InvitationAlreadyResolved`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Conflict, """{"code":"INVITATION_ALREADY_RESOLVED"}"""),
        ).acceptInvitation("inv1")

        assertIs<Result.Failure<FriendGroupError>>(result)
        assertEquals(FriendGroupError.InvitationAlreadyResolved, result.error)
    }

    @Test
    fun `invite 404 maps to InvitedUserNotFound`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.NotFound, """{"code":"INVITED_USER_NOT_FOUND"}"""),
        ).invite("g1", "ghost@test.com")

        assertIs<Result.Failure<FriendGroupError>>(result)
        assertEquals(FriendGroupError.InvitedUserNotFound, result.error)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = repository(client).listMine()

        assertIs<Result.Failure<FriendGroupError>>(result)
        assertEquals(FriendGroupError.Network, result.error)
    }
}
