package eu.ejdr.application.features.campaign.usecase

import eu.ejdr.application.features.campaign.abstraction.repository.CampaignRepository
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.error.CampaignError

class DeleteCampaignUseCaseImpl(
    private val repository: CampaignRepository,
) : DeleteCampaignUseCase {
    override suspend fun invoke(id: String): Result<Unit, CampaignError> = repository.delete(id)
}
