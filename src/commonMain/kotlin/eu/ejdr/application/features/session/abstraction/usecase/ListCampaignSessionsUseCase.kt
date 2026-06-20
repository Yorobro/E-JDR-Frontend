package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : liste les sessions d'une campagne. */
fun interface ListCampaignSessionsUseCase {
    suspend operator fun invoke(campaignId: String): Result<List<Session>, SessionError>
}
