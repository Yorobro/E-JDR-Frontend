package eu.ejdr.application.features.campaign.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

/** Use case : liste les campagnes de l'utilisateur courant (maître du jeu). */
fun interface ListCampaignsUseCase {
    suspend operator fun invoke(): Result<List<Campaign>, CampaignError>
}
