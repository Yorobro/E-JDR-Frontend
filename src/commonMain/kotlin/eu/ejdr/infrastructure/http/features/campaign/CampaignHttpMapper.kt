package eu.ejdr.infrastructure.http.features.campaign

import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError
import eu.ejdr.infrastructure.http.features.campaign.dto.CampaignDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut HTTP) de la feature campagnes
 * vers le domaine. Frontière isolant la couche application des détails du protocole.
 * Sans état : toutes les opérations sont pures.
 */
object CampaignHttpMapper {

    /**
     * Convertit une campagne reçue de l'API en entité domaine.
     *
     * @param dto Campagne JSON désérialisée.
     * @return La [Campaign] correspondante.
     */
    fun toCampaign(dto: CampaignDto): Campaign =
        Campaign(
            id = dto.id,
            name = dto.name,
            gameMasterId = dto.gameMasterId,
            createdAt = dto.createdAt,
        )

    /**
     * Traduit un échec HTTP en erreur métier campagne.
     *
     * Le **code applicatif** prime quand il est présent (contrat partagé avec le backend) ;
     * à défaut on retombe sur le statut HTTP, puis sur [CampaignError.Unknown].
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel.
     * @param message Message d'erreur lisible éventuel.
     * @return La [CampaignError] du domaine correspondante.
     */
    fun toError(status: HttpStatusCode, code: String?, message: String?): CampaignError =
        when (code) {
            "INVALID_CAMPAIGN_NAME" -> CampaignError.InvalidName
            "CAMPAIGN_NOT_FOUND" -> CampaignError.NotFound
            "CAMPAIGN_ACCESS_DENIED" -> CampaignError.AccessDenied
            else -> when (status) {
                HttpStatusCode.NotFound -> CampaignError.NotFound
                HttpStatusCode.Forbidden -> CampaignError.AccessDenied
                HttpStatusCode.BadRequest -> CampaignError.InvalidName
                else -> CampaignError.Unknown(message ?: code ?: status.description)
            }
        }
}
