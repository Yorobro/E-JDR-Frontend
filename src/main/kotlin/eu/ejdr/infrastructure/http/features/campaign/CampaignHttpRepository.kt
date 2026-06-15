package eu.ejdr.infrastructure.http.features.campaign

import eu.ejdr.application.features.campaign.abstraction.repository.CampaignRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.campaign.dto.CampaignDto
import eu.ejdr.infrastructure.http.features.campaign.dto.CampaignListResponseDto
import eu.ejdr.infrastructure.http.features.campaign.dto.CreateCampaignRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implémentation HTTP de [CampaignRepository] dialoguant avec les routes `/campaigns`.
 *
 * La session repose sur des cookies HttpOnly posés et lus par le serveur via le [HttpClient]
 * (refresh-on-401 transparent). Toute erreur de transport est convertie en
 * [CampaignError.Network] ; les échecs applicatifs passent par [CampaignHttpMapper.toError].
 *
 * @property client Client HTTP configuré (cookies + JSON).
 * @property config Configuration applicative (URL de base).
 */
class CampaignHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
) : CampaignRepository {

    /**
     * Liste les campagnes du maître du jeu courant via `GET /campaigns`.
     *
     * @return [Result.Success] avec la liste des campagnes, sinon une [CampaignError].
     */
    override suspend fun list(): Result<List<Campaign>, CampaignError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/campaigns")
            if (response.status.isSuccess()) {
                val body = response.body<CampaignListResponseDto>()
                Result.Success(body.campaigns.map(CampaignHttpMapper::toCampaign))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CampaignError.Network) }

    /**
     * Crée une campagne via `POST /campaigns`.
     *
     * @param name Nom de la campagne à créer.
     * @return [Result.Success] avec la campagne créée, sinon une [CampaignError]
     * (ex. [CampaignError.InvalidName]).
     */
    override suspend fun create(name: String): Result<Campaign, CampaignError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/campaigns") {
                contentType(ContentType.Application.Json)
                setBody(CreateCampaignRequestDto(name))
            }
            if (response.status.isSuccess()) {
                Result.Success(CampaignHttpMapper.toCampaign(response.body<CampaignDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CampaignError.Network) }

    /**
     * Supprime une campagne via `DELETE /campaigns/{id}`.
     *
     * @param id Identifiant de la campagne à supprimer.
     * @return [Result.Success] si la suppression réussit (204), sinon une [CampaignError]
     * ([CampaignError.NotFound] / [CampaignError.AccessDenied]).
     */
    override suspend fun delete(id: String): Result<Unit, CampaignError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/campaigns/$id")
            if (response.status.isSuccess()) {
                Result.Success(Unit)
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CampaignError.Network) }

    /**
     * Lit le corps d'erreur (best-effort) et le traduit en [CampaignError] via le mapper.
     *
     * @param response Réponse HTTP en échec.
     * @return Le [Result.Failure] porteur de l'erreur métier.
     */
    private suspend fun failure(response: HttpResponse): Result.Failure<CampaignError> {
        val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
        return Result.Failure(CampaignHttpMapper.toError(response.status, err?.code, err?.message))
    }
}
