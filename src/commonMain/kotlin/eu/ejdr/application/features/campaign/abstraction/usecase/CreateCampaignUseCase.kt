package eu.ejdr.application.features.campaign.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

/**
 * Use case : crée une campagne dans le groupe indiqué ; l'utilisateur courant en devient le MJ.
 */
fun interface CreateCampaignUseCase {
    suspend operator fun invoke(name: String, groupId: String): Result<Campaign, CampaignError>
}
