package eu.ejdr.application.features.reference.usecase

import eu.ejdr.application.features.reference.abstraction.repository.ReferenceRepository
import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UpdateReferenceItemUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError

/**
 * Implémentations des use cases de la feature référence : orchestration triviale (chaque use case
 * délègue au [ReferenceRepository]). Regroupées car chacune est une simple délégation.
 */

class ListReferenceItemsUseCaseImpl(
    private val repository: ReferenceRepository,
) : ListReferenceItemsUseCase {
    override suspend fun invoke(
        type: ReferenceType,
        groupId: String,
    ): Result<List<ReferenceItem>, ReferenceError> = repository.list(type, groupId)
}

class CreateReferenceItemUseCaseImpl(
    private val repository: ReferenceRepository,
) : CreateReferenceItemUseCase {
    override suspend fun invoke(
        type: ReferenceType,
        name: String,
        groupId: String,
        stat: String?,
        bonus: Int?,
        competenceIds: List<String>,
        protectionPoints: Int?,
        description: String?,
    ): Result<ReferenceItem, ReferenceError> =
        repository.create(
            type,
            name,
            groupId,
            stat,
            bonus,
            competenceIds,
            protectionPoints,
            description,
        )
}

class UpdateReferenceItemUseCaseImpl(
    private val repository: ReferenceRepository,
) : UpdateReferenceItemUseCase {
    override suspend fun invoke(
        type: ReferenceType,
        itemId: String,
        name: String,
        groupId: String,
        stat: String?,
        bonus: Int?,
        competenceIds: List<String>,
        protectionPoints: Int?,
        description: String?,
    ): Result<ReferenceItem, ReferenceError> =
        repository.update(
            type,
            itemId,
            name,
            groupId,
            stat,
            bonus,
            competenceIds,
            protectionPoints,
            description,
        )
}

class DeleteReferenceItemUseCaseImpl(
    private val repository: ReferenceRepository,
) : DeleteReferenceItemUseCase {
    override suspend fun invoke(
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> = repository.delete(type, itemId)
}

class ListSheetReferencesUseCaseImpl(
    private val repository: ReferenceRepository,
) : ListSheetReferencesUseCase {
    override suspend fun invoke(
        sheetId: String,
        type: ReferenceType,
    ): Result<List<ReferenceItem>, ReferenceError> = repository.listLinked(sheetId, type)
}

class LinkSheetReferenceUseCaseImpl(
    private val repository: ReferenceRepository,
) : LinkSheetReferenceUseCase {
    override suspend fun invoke(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> = repository.link(sheetId, type, itemId)
}

class UnlinkSheetReferenceUseCaseImpl(
    private val repository: ReferenceRepository,
) : UnlinkSheetReferenceUseCase {
    override suspend fun invoke(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> = repository.unlink(sheetId, type, itemId)
}
