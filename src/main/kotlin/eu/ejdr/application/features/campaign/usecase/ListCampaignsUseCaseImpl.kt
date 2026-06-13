package eu.ejdr.application.features.campaign.usecase

import eu.ejdr.application.features.campaign.abstraction.repository.CampaignRepository
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

class ListCampaignsUseCaseImpl(
    private val repository: CampaignRepository,
) : ListCampaignsUseCase {
    override suspend fun invoke(): Result<List<Campaign>, CampaignError> = repository.list()
}
