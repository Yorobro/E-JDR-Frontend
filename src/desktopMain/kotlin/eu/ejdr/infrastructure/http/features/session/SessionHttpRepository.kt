package eu.ejdr.infrastructure.http.features.session

import eu.ejdr.application.features.session.abstraction.repository.SessionRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.session.dto.CreateSessionRequestDto
import eu.ejdr.infrastructure.http.features.session.dto.SessionDto
import eu.ejdr.infrastructure.http.features.session.dto.SessionListResponseDto
import eu.ejdr.infrastructure.http.features.session.dto.UpdateSessionRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implémentation HTTP de [SessionRepository] dialoguant avec les routes
 * `/campaigns/{id}/sessions` (liste, création) et `/sessions/{id}` (détail, mise à jour,
 * suppression).
 *
 * La session repose sur des cookies HttpOnly posés et lus par le serveur via le [HttpClient]
 * (refresh-on-401 transparent). Toute erreur de transport est convertie en
 * [SessionError.Network] ; les échecs applicatifs passent par [SessionHttpMapper.toError].
 *
 * @property client Client HTTP configuré (cookies + JSON).
 * @property config Configuration applicative (URL de base).
 */
class SessionHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
) : SessionRepository {

    override suspend fun listByCampaign(campaignId: String): Result<List<Session>, SessionError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/campaigns/$campaignId/sessions")
            if (response.status.isSuccess()) {
                val body = response.body<SessionListResponseDto>()
                Result.Success(body.sessions.map(SessionHttpMapper::toSession))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(SessionError.Network) }

    override suspend fun create(
        campaignId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/campaigns/$campaignId/sessions") {
                contentType(ContentType.Application.Json)
                setBody(CreateSessionRequestDto(title, date))
            }
            if (response.status.isSuccess()) {
                Result.Success(SessionHttpMapper.toSession(response.body<SessionDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(SessionError.Network) }

    override suspend fun get(sessionId: String): Result<Session, SessionError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/sessions/$sessionId")
            if (response.status.isSuccess()) {
                Result.Success(SessionHttpMapper.toSession(response.body<SessionDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(SessionError.Network) }

    override suspend fun update(
        sessionId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError> =
        runCatchingCancellable {
            val response = client.put("${config.baseUrl}/sessions/$sessionId") {
                contentType(ContentType.Application.Json)
                setBody(UpdateSessionRequestDto(title, date))
            }
            if (response.status.isSuccess()) {
                Result.Success(SessionHttpMapper.toSession(response.body<SessionDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(SessionError.Network) }

    override suspend fun delete(sessionId: String): Result<Unit, SessionError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/sessions/$sessionId")
            if (response.status.isSuccess()) {
                Result.Success(Unit)
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(SessionError.Network) }

    /**
     * Lit le corps d'erreur (best-effort) et le traduit en [SessionError] via le mapper.
     *
     * @param response Réponse HTTP en échec.
     * @return Le [Result.Failure] porteur de l'erreur métier.
     */
    private suspend fun failure(response: HttpResponse): Result.Failure<SessionError> {
        val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
        return Result.Failure(SessionHttpMapper.toError(response.status, err?.code, err?.message))
    }
}
