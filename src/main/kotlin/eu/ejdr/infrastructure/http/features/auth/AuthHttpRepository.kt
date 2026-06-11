package eu.ejdr.infrastructure.http.features.auth

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.auth.dto.AuthRequestDto
import eu.ejdr.infrastructure.http.features.auth.dto.AuthResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implémentation HTTP de [AuthRepository] dialoguant avec les routes
 * `/auth/login`, `/auth/register`, `/auth/refresh` et `/auth/logout`.
 *
 * Modèle d'authentification : la session repose sur des cookies HttpOnly posés
 * et lus par le serveur via le [HttpClient]. Ce code ne manipule donc jamais le
 * token directement ; seul le refresh_token est persisté chiffré par
 * [SecureCookiesStorage] pour permettre la restauration de session.
 *
 * Toute erreur de transport est convertie en [AuthError.Network] ; les échecs
 * applicatifs passent par [AuthHttpMapper.toAuthError].
 *
 * @property client Client HTTP configuré (cookies + JSON).
 * @property config Configuration applicative (URL de base).
 * @property mapper Traducteur DTO/statut HTTP vers le domaine.
 * @property sessionPersistence Port de persistance de session, sollicité pour effacer
 * le refresh_token lors d'un refresh échoué ou d'une déconnexion.
 */
class AuthHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
    private val mapper: AuthHttpMapper,
    private val sessionPersistence: SessionPersistence,
) : AuthRepository {

    /**
     * Authentifie un utilisateur existant via `/auth/login`.
     *
     * @param credentials Identifiants saisis par l'utilisateur.
     * @return [Result.Success] avec l'[User] connecté, sinon une [AuthError].
     */
    override suspend fun login(credentials: Credentials): Result<User, AuthError> =
        authenticate("/auth/login", credentials)

    /**
     * Crée un compte puis ouvre la session via `/auth/register`.
     *
     * @param credentials Identifiants du nouveau compte.
     * @return [Result.Success] avec l'[User] créé, sinon une [AuthError].
     */
    override suspend fun register(credentials: Credentials): Result<User, AuthError> =
        authenticate("/auth/register", credentials)

    /**
     * Logique commune de login/register : poste les identifiants, puis traduit
     * la réponse (succès -> [User], échec -> [AuthError] via le mapper). Les
     * cookies de session éventuels sont gérés de façon transparente par le
     * [HttpClient].
     *
     * @param path Chemin de l'endpoint d'authentification.
     * @param credentials Identifiants à transmettre.
     * @return Le résultat typé de l'opération.
     */
    private suspend fun authenticate(path: String, credentials: Credentials): Result<User, AuthError> =
        runCatchingCancellable {
            val response: HttpResponse = client.post("${config.baseUrl}$path") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequestDto(credentials.email, credentials.password))
            }
            if (response.status.isSuccess()) {
                Result.Success(mapper.toUser(response.body<AuthResponseDto>()))
            } else {
                val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
                Result.Failure(mapper.toAuthError(response.status, err?.code, err?.message))
            }
        }.getOrElse { Result.Failure(AuthError.Network) }

    /**
     * Renouvelle la session via `/auth/refresh` à partir du refresh_token.
     *
     * En cas d'échec, le cookie persisté est effacé pour éviter de retenter
     * indéfiniment une restauration avec un jeton invalide.
     *
     * @return [Result.Success] si la session est renouvelée, sinon
     * [AuthError.SessionExpired] (échec serveur) ou [AuthError.Network].
     */
    override suspend fun refresh(): Result<User, AuthError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/auth/refresh")
            if (response.status.isSuccess()) {
                Result.Success(mapper.toUser(response.body<AuthResponseDto>()))
            } else {
                sessionPersistence.clearPersisted()
                Result.Failure(AuthError.SessionExpired)
            }
        }.getOrElse { Result.Failure(AuthError.Network) }

    /**
     * Déconnecte l'utilisateur via `/auth/logout`.
     *
     * Le cookie persisté est systématiquement effacé, que l'appel serveur
     * réussisse ou échoue : la déconnexion locale doit toujours aboutir. L'appel
     * renvoie donc toujours un succès.
     *
     * @return Toujours [Result.Success].
     */
    override suspend fun logout(): Result<Unit, AuthError> {
        // L'appel serveur est best-effort : on ignore tout échec réseau...
        runCatchingCancellable { client.post("${config.baseUrl}/auth/logout") }
        // ...mais l'effacement local doit TOUJOURS aboutir, d'où sa place hors du bloc.
        sessionPersistence.clearPersisted()
        return Result.Success(Unit)
    }

    /**
     * Récupère le profil courant via `GET /me` (route protégée).
     *
     * Si un 401 arrive ici, c'est que l'intercepteur de refresh silencieux a déjà
     * échoué (il a alors effacé la session persistée) : on traduit en
     * [AuthError.SessionExpired] pour que la présentation ramène à la connexion.
     *
     * @return [Result.Success] avec l'[User] courant, sinon une [AuthError].
     */
    override suspend fun me(): Result<User, AuthError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/me")
            when {
                response.status.isSuccess() ->
                    Result.Success(mapper.toUser(response.body<AuthResponseDto>()))

                response.status == HttpStatusCode.Unauthorized ->
                    Result.Failure(AuthError.SessionExpired)

                else -> {
                    val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
                    Result.Failure(mapper.toAuthError(response.status, err?.code, err?.message))
                }
            }
        }.getOrElse { Result.Failure(AuthError.Network) }

    /**
     * Indique si une session restaurable (refresh_token chiffré) existe sur disque.
     *
     * @return `true` si un refresh_token a été persisté, sinon `false`.
     */
    override fun hasPersistedSession(): Boolean = sessionPersistence.hasPersistedSession()
}
