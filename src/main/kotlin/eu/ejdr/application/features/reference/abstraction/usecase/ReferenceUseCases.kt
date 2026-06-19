package eu.ejdr.application.features.reference.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError

/** Use case : liste les éléments du catalogue du groupe actif pour un type. */
fun interface ListReferenceItemsUseCase {
    suspend operator fun invoke(
        type: ReferenceType,
        groupId: String,
    ): Result<List<ReferenceItem>, ReferenceError>
}

/**
 * Use case : crée un élément dans le catalogue d'un type pour le groupe actif (admin requis).
 *
 * [stat]/[bonus] s'appliquent aux formations/peuples, [competenceIds] à la seule formation ; les
 * autres types passent `null`/`emptyList`.
 */
fun interface CreateReferenceItemUseCase {
    suspend operator fun invoke(
        type: ReferenceType,
        name: String,
        groupId: String,
        stat: String?,
        bonus: Int?,
        competenceIds: List<String>,
    ): Result<ReferenceItem, ReferenceError>
}

/** Use case : supprime un élément du catalogue. */
fun interface DeleteReferenceItemUseCase {
    suspend operator fun invoke(type: ReferenceType, itemId: String): Result<Unit, ReferenceError>
}

/** Use case : liste les éléments (d'un type liable) rattachés à une fiche. */
fun interface ListSheetReferencesUseCase {
    suspend operator fun invoke(
        sheetId: String,
        type: ReferenceType,
    ): Result<List<ReferenceItem>, ReferenceError>
}

/** Use case : rattache un élément à une fiche (N‑N). */
fun interface LinkSheetReferenceUseCase {
    suspend operator fun invoke(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError>
}

/** Use case : détache un élément d'une fiche (N‑N). */
fun interface UnlinkSheetReferenceUseCase {
    suspend operator fun invoke(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError>
}
