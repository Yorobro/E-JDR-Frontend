package eu.ejdr.infrastructure.http.features.campaign

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError
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
import kotlin.test.assertTrue

/**
 * Tests de l'adapter HTTP [CampaignHttpRepository] avec le MockEngine de Ktor.
 *
 * Couvre la sérialisation des requêtes, la désérialisation des réponses/erreurs, la
 * traduction des statuts/codes en [CampaignError] et la gestion des échecs réseau.
 */
class CampaignHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-campaign-test").toFile()
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

    private fun repository(client: HttpClient) = CampaignHttpRepository(client, config)

    @Test
    fun `list scopes the request by the active group`() = runTest {
        val body = """
            {"campaigns":[{"id":"c-1","name":"Alpha","gameMasterId":"gm-1",
             "createdAt":"2026-06-13T10:00:00.000Z"}]}
        """.trimIndent()
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

        assertIs<Result.Success<List<Campaign>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("Alpha", result.value.first().name)
        assertTrue(requestedUrl.contains("groupId=g-1"), "URL should carry groupId: $requestedUrl")
    }

    @Test
    fun `create sends name and group in the body`() = runTest {
        val body =
            """{"id":"c-1","name":"Alpha","gameMasterId":"gm-1","createdAt":"2026-06-13T10:00:00.000Z"}"""
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

        val result = repository(client).create("Alpha", "g-1")

        assertIs<Result.Success<Campaign>>(result)
        assertEquals("c-1", result.value.id)
        assertTrue(requestBody.contains("\"groupId\":\"g-1\""), "Body should carry groupId: $requestBody")
        assertTrue(requestBody.contains("\"name\":\"Alpha\""), "Body should carry name: $requestBody")
    }

    @Test
    fun `create 400 maps to InvalidName`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.BadRequest, """{"code":"INVALID_CAMPAIGN_NAME"}"""),
        ).create("", "g-1")

        assertIs<Result.Failure<CampaignError>>(result)
        assertEquals(CampaignError.InvalidName, result.error)
    }

    @Test
    fun `delete success on 204`() = runTest {
        val result = repository(clientReturning(HttpStatusCode.NoContent, "")).delete("c-1")

        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `delete 404 maps to NotFound`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.NotFound, """{"code":"CAMPAIGN_NOT_FOUND"}"""),
        ).delete("ghost")

        assertIs<Result.Failure<CampaignError>>(result)
        assertEquals(CampaignError.NotFound, result.error)
    }

    @Test
    fun `delete 403 maps to AccessDenied`() = runTest {
        val result = repository(
            clientReturning(HttpStatusCode.Forbidden, """{"code":"CAMPAIGN_ACCESS_DENIED"}"""),
        ).delete("c-1")

        assertIs<Result.Failure<CampaignError>>(result)
        assertEquals(CampaignError.AccessDenied, result.error)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = repository(client).list("g-1")

        assertIs<Result.Failure<CampaignError>>(result)
        assertEquals(CampaignError.Network, result.error)
    }
}
