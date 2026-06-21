package eu.ejdr.application.features.campaign.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

/** Use case : liste les campagnes du groupe actif (membre requis côté serveur). */
fun interface ListCampaignsUseCase {
    suspend operator fun invoke(groupId: String): Result<List<Campaign>, CampaignError>
}
