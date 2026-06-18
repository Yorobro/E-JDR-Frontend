package eu.ejdr.application.features.friendgroup.usecase

import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase

class GetActiveGroupIdUseCaseImpl(private val repository: ActiveGroupRepository) : GetActiveGroupIdUseCase {
    override suspend fun invoke(): String? = repository.getActiveGroupId()
}

class SetActiveGroupIdUseCaseImpl(private val repository: ActiveGroupRepository) : SetActiveGroupIdUseCase {
    override suspend fun invoke(id: String?) = repository.setActiveGroupId(id)
}
