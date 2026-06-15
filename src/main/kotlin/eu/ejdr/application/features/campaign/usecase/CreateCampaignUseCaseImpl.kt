package eu.ejdr.application.features.campaign.usecase

import eu.ejdr.application.features.campaign.abstraction.repository.CampaignRepository
import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError

class CreateCampaignUseCaseImpl(
    private val repository: CampaignRepository,
) : CreateCampaignUseCase {
    override suspend fun invoke(name: String): Result<Campaign, CampaignError> =
        repository.create(name)
}
