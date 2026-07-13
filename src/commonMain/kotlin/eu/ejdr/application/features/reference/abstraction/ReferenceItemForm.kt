package eu.ejdr.application.features.reference.abstraction

import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus

/**
 * Champs saisis pour créer ou modifier un élément de référence.
 *
 * Regroupés dans un objet plutôt que passés positionnellement : la création en comptait déjà **8**
 * et la modification **9**, dont deux collections et deux notions de « stat » voisines. Ajouter un
 * paramètre de plus rendait une **transposition d'arguments indétectable à la compilation** — et
 * obligeait à répercuter la signature dans les deux `ReferenceListPage` (desktop **et** Android),
 * où les lambdas sont recopiées à l'identique.
 *
 * Les champs ne concernent qu'une partie des types ; les autres les laissent à leur défaut :
 *
 * | Champ | Type concerné |
 * |---|---|
 * | [stat] + [bonus] | **formation** (au plus **un** bonus) |
 * | [statBonuses] | **peuple** (**0..N** bonus, au plus un par stat) |
 * | [competenceIds] | **formation** |
 * | [protectionPoints] | **armure** |
 * | [description] | **sort**, **miracle** |
 *
 * L'asymétrie formation/peuple est celle du backend, et elle est assumée.
 *
 * @property name Nom de l'élément (obligatoire).
 * @property stat Statistique ciblée par le bonus d'une formation, ou `null`.
 * @property bonus Montant du bonus de la formation, ou `null`.
 * @property statBonuses Bonus d'un peuple (au plus un par statistique).
 * @property competenceIds Compétences apportées par une formation.
 * @property protectionPoints Points de protection d'une armure.
 * @property description Description libre d'un sort ou d'un miracle.
 */
data class ReferenceItemForm(
    val name: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val statBonuses: List<ReferenceStatBonus> = emptyList(),
    val competenceIds: List<String> = emptyList(),
    val protectionPoints: Int? = null,
    val description: String? = null,
)
