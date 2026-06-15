package eu.ejdr.application.features.campaign.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.error.CampaignError

/** Use case : supprime une campagne dont l'utilisateur courant est le maître du jeu. */
fun interface DeleteCampaignUseCase {
    suspend operator fun invoke(id: String): Result<Unit, CampaignError>
}
