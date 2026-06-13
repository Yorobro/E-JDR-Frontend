package eu.ejdr.application.features.campaign.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

/** Use case : crée une campagne dont l'utilisateur courant devient le maître du jeu. */
fun interface CreateCampaignUseCase {
    suspend operator fun invoke(name: String): Result<Campaign, CampaignError>
}
