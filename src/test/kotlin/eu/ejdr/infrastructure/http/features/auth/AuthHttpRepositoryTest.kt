package eu.ejdr.infrastructure.http.features.auth

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
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
 * Tests du véritable adapter HTTP [AuthHttpRepository] avec le MockEngine de Ktor.
 *
 * Couvre ce que les tests de use case (qui mockent le repository) ne peuvent pas voir :
 * la sérialisation des requêtes, la désérialisation des réponses/erreurs, la traduction
 * des statuts en [AuthError], et la gestion des échecs réseau/clear-on-fail.
 */
class AuthHttpRepositoryTest {
    private val tmpDir = Files.createTempDirectory("ejdr-repo-test").toFile()
    private val config =
        AppConfig(
            baseUrl = "http://localhost:3000",
            dataDir = tmpDir,
            enableHttpLogging = false,
        )
    private val cookiesStorage =
        SecureCookiesStorage(
            tmpDir,
            CookieCipher(KeyStoreProvider(tmpDir)),
            config.baseUrl,
            AcceptAllCookiesStorage(),
        )

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    /** Construit un client Ktor réel dont le moteur renvoie une réponse fixée. */
    private fun clientReturning(
        status: HttpStatusCode,
        body: String,
    ): HttpClient {
        val engine =
            MockEngine {
                respond(
                    content = ByteReadChannel(body),
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return HttpClient(engine) {
            install(HttpCookies) { storage = cookiesStorage }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }

    /**
     * Client Ktor dont le moteur répond différemment selon le chemin de la requête.
     * Utile pour `refresh()` qui enchaîne `/auth/refresh` puis `/me`.
     */
    private fun clientByPath(
        responder: (path: String) -> Pair<HttpStatusCode, String>,
    ): HttpClient {
        val engine =
            MockEngine { request ->
                val (status, body) = responder(request.url.encodedPath)
                respond(
                    content = ByteReadChannel(body),
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return HttpClient(engine) {
            install(HttpCookies) { storage = cookiesStorage }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun repository(client: HttpClient) = AuthHttpRepository(client, config, AuthHttpMapper, cookiesStorage)

    private val creds = Credentials("user@test.com", "password123")

    @Test
    fun `login success maps body to User`() =
        runTest {
            val repo =
                repository(
                    clientReturning(HttpStatusCode.OK, """{"userId":"u-1","email":"user@test.com"}"""),
                )

            val result = repo.login(creds)

            assertIs<Result.Success<*>>(result)
            assertEquals("user@test.com", (result.value as eu.ejdr.domain.features.auth.entities.User).email)
        }

    @Test
    fun `login 401 maps to InvalidCredentials`() =
        runTest {
            val repo =
                repository(
                    clientReturning(HttpStatusCode.Unauthorized, """{"code":"INVALID_CREDENTIALS"}"""),
                )

            val result = repo.login(creds)

            assertIs<Result.Failure<AuthError>>(result)
            assertEquals(AuthError.InvalidCredentials, result.error)
        }

    @Test
    fun `register 409 maps to EmailAlreadyUsed`() =
        runTest {
            val repo =
                repository(
                    clientReturning(HttpStatusCode.Conflict, """{"code":"EMAIL_ALREADY_USED"}"""),
                )

            val result = repo.register(creds)

            assertIs<Result.Failure<AuthError>>(result)
            assertEquals(AuthError.EmailAlreadyUsed, result.error)
        }

    @Test
    fun `refresh success fetches the user via me (refresh body has no user)`() =
        runTest {
            // Contrat réel : /auth/refresh renvoie { "message": ... } (pas l'utilisateur).
            // L'utilisateur est ensuite récupéré via GET /me.
            val repo =
                repository(
                    clientByPath { path ->
                        when {
                            path.endsWith("/auth/refresh") ->
                                HttpStatusCode.OK to """{"message":"Jetons rafraîchis."}"""
                            path.endsWith("/me") ->
                                HttpStatusCode.OK to """{"userId":"u-1","email":"user@test.com","createdAt":"2026-06-10T08:00:00.000Z"}"""
                            else -> HttpStatusCode.NotFound to "{}"
                        }
                    },
                )

            val result = repo.refresh()

            assertIs<Result.Success<*>>(result)
            assertEquals("user@test.com", (result.value as eu.ejdr.domain.features.auth.entities.User).email)
        }

    @Test
    fun `refresh failure clears persisted session and returns SessionExpired`() =
        runTest {
            val repo = repository(clientReturning(HttpStatusCode.Unauthorized, """{"code":"x"}"""))

            val result = repo.refresh()

            assertIs<Result.Failure<AuthError>>(result)
            assertEquals(AuthError.SessionExpired, result.error)
        }

    @Test
    fun `logout always succeeds even on server error`() =
        runTest {
            val repo =
                repository(
                    clientReturning(HttpStatusCode.InternalServerError, "boom"),
                )

            val result = repo.logout()

            assertIs<Result.Success<Unit>>(result)
        }

    @Test
    fun `transport failure maps to Network error`() =
        runTest {
            val engine = MockEngine { throw java.io.IOException("connection refused") }
            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
            val result = repository(client).login(creds)

            assertIs<Result.Failure<AuthError>>(result)
            assertEquals(AuthError.Network, result.error)
        }

    @Test
    fun `me success maps body to User`() =
        runTest {
            val repo =
                repository(
                    clientReturning(
                        HttpStatusCode.OK,
                        """{"userId":"u-1","email":"user@test.com","createdAt":"2026-06-10T08:00:00.000Z"}""",
                    ),
                )

            val result = repo.me()

            assertIs<Result.Success<*>>(result)
            assertEquals("user@test.com", (result.value as eu.ejdr.domain.features.auth.entities.User).email)
        }

    @Test
    fun `me 401 maps to SessionExpired`() =
        runTest {
            val repo =
                repository(
                    clientReturning(
                        HttpStatusCode.Unauthorized,
                        """{"code":"UNAUTHENTICATED","message":"Authentification requise."}""",
                    ),
                )

            val result = repo.me()

            assertIs<Result.Failure<*>>(result)
            assertEquals(AuthError.SessionExpired, result.error)
        }

    @Test
    fun `me 401 USER_NOT_FOUND (compte supprime) maps to SessionExpired`() =
        runTest {
            val repo =
                repository(
                    clientReturning(
                        HttpStatusCode.Unauthorized,
                        """{"code":"USER_NOT_FOUND","message":"Utilisateur introuvable."}""",
                    ),
                )

            val result = repo.me()

            assertIs<Result.Failure<*>>(result)
            assertEquals(AuthError.SessionExpired, result.error)
        }

    @Test
    fun `me network failure maps to Network`() =
        runTest {
            val engine = MockEngine { throw java.io.IOException("connexion refusée") }
            val client =
                HttpClient(engine) {
                    install(HttpCookies) { storage = cookiesStorage }
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            },
                        )
                    }
                }

            val result = repository(client).me()

            assertIs<Result.Failure<*>>(result)
            assertEquals(AuthError.Network, result.error)
        }
}
