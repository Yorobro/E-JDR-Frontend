package eu.ejdr.infrastructure.http.features.update

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de l'adapter HTTP réel [UpdateHttpRepository] avec le MockEngine de Ktor.
 *
 * Couvre ce que les tests de `CheckUpdateUseCase` (qui mockent le repository) ne voient pas :
 * la désérialisation de la réponse GitHub, la sélection de l'asset (`.exe` puis `.msi`) et la
 * dégradation silencieuse (`null`) sur statut non-2xx ou échec transport.
 */
class UpdateHttpRepositoryTest {

    private fun repositoryReturning(status: HttpStatusCode, body: String): UpdateHttpRepository {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return UpdateHttpRepository(client)
    }

    @Test
    fun `parses tag, html url and prefers the exe asset`() = runTest {
        val body = """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://github.com/owner/repo/releases/tag/v1.2.3",
              "assets": [
                {"name": "E-JDR-1.2.3.msi", "browser_download_url": "https://example.com/app.msi"},
                {"name": "E-JDR-1.2.3.exe", "browser_download_url": "https://example.com/app.exe"},
                {"name": "E-JDR-1.2.3.exe.sha256", "browser_download_url": "https://example.com/app.exe.sha256"}
              ]
            }
        """.trimIndent()

        val info = repositoryReturning(HttpStatusCode.OK, body).fetchLatestRelease()

        assertEquals("v1.2.3", info?.version)
        assertEquals("https://github.com/owner/repo/releases/tag/v1.2.3", info?.releaseUrl)
        assertEquals("https://example.com/app.exe", info?.downloadUrl)
        // L'empreinte SHA-256 de l'installeur retenu est associée (asset "<installeur>.sha256").
        assertEquals("https://example.com/app.exe.sha256", info?.sha256Url)
    }

    @Test
    fun `leaves the sha256 url null when no checksum asset matches the installer`() = runTest {
        val body = """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://example.com/release",
              "assets": [
                {"name": "E-JDR-1.2.3.exe", "browser_download_url": "https://example.com/app.exe"}
              ]
            }
        """.trimIndent()

        val info = repositoryReturning(HttpStatusCode.OK, body).fetchLatestRelease()

        assertEquals("https://example.com/app.exe", info?.downloadUrl)
        assertNull(info?.sha256Url)
    }

    @Test
    fun `falls back to the msi asset when no exe is present`() = runTest {
        val body = """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://example.com/release",
              "assets": [
                {"name": "E-JDR-1.2.3.msi", "browser_download_url": "https://example.com/app.msi"}
              ]
            }
        """.trimIndent()

        val info = repositoryReturning(HttpStatusCode.OK, body).fetchLatestRelease()

        assertEquals("https://example.com/app.msi", info?.downloadUrl)
    }

    @Test
    fun `returns a null download url when no installer asset is present`() = runTest {
        val body = """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://example.com/release",
              "assets": [
                {"name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt"}
              ]
            }
        """.trimIndent()

        val info = repositoryReturning(HttpStatusCode.OK, body).fetchLatestRelease()

        assertEquals("v1.2.3", info?.version)
        assertNull(info?.downloadUrl)
    }

    @Test
    fun `returns null on a non-success status`() = runTest {
        val info = repositoryReturning(HttpStatusCode.NotFound, """{"message":"Not Found"}""")
            .fetchLatestRelease()

        assertNull(info)
    }

    @Test
    fun `returns null on a transport failure`() = runTest {
        val engine = MockEngine { throw java.io.IOException("connexion refusée") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        assertNull(UpdateHttpRepository(client).fetchLatestRelease())
    }

    @Test
    fun `returns null on a malformed body`() = runTest {
        val info = repositoryReturning(HttpStatusCode.OK, "not json at all").fetchLatestRelease()

        assertNull(info)
    }
}
