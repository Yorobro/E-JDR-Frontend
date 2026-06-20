package eu.ejdr.application.features.campaign.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

/**
 * Port d'accès aux campagnes : abstraction des opérations distantes (API REST).
 *
 * Implémenté par la couche infrastructure (HTTP) ; consommé par les use cases sans
 * dépendre des détails techniques. Toutes les opérations renvoient un [Result] :
 * aucune exception ne doit remonter.
 */
interface CampaignRepository {
    /**
     * Liste les campagnes du groupe indiqué (l'utilisateur doit en être membre côté serveur).
     *
     * @param groupId identifiant du groupe actif.
     * @return la liste des campagnes, ou une [CampaignError] en cas d'échec.
     */
    suspend fun list(groupId: String): Result<List<Campaign>, CampaignError>

    /**
     * Crée une campagne dans le groupe indiqué ; l'utilisateur courant en devient le maître du jeu.
     *
     * @param name nom de la campagne à créer.
     * @param groupId identifiant du groupe actif auquel rattacher la campagne.
     * @return la campagne créée, ou une [CampaignError] (ex. [CampaignError.InvalidName]).
     */
    suspend fun create(name: String, groupId: String): Result<Campaign, CampaignError>

    /**
     * Supprime une campagne dont l'utilisateur courant est le maître du jeu.
     *
     * @param id identifiant de la campagne à supprimer.
     * @return [Unit] si la suppression réussit, ou une [CampaignError]
     * ([CampaignError.NotFound] / [CampaignError.AccessDenied]).
     */
    suspend fun delete(id: String): Result<Unit, CampaignError>
}
