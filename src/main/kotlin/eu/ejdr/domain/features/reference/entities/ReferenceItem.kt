package eu.ejdr.domain.features.reference.entities

/**
 * Élément de référence créé par l'utilisateur (formation, peuple, arme, armure, compétence ou
 * équipement). Conteneur de données pur (domaine front anémique) : représente un élément tel que
 * reçu du serveur. Le **type** n'est pas porté par l'entité (toutes les catégories ont la même
 * forme) mais par le contexte d'appel (cf. [ReferenceType]).
 *
 * @property id Identifiant unique stable de l'élément.
 * @property name Nom affiché de l'élément.
 * @property createdAt Date de création au format ISO 8601 (telle que renvoyée par l'API).
 * @property stat Statistique associée (formation/peuple) — slug serveur
 *   (`dexterite`/`intelligence`/`perception`/`social`/`vigueur`), ou `null` si aucune.
 * @property bonus Montant du bonus appliqué à la [stat] (formation/peuple), ou `null` si aucune stat.
 * @property competenceIds Identifiants des compétences liées (formation uniquement ; vide sinon).
 * @property protectionPoints Points de protection apportés (armure uniquement), ou `null` sinon.
 * @property description Description libre (sort/miracle uniquement), ou `null` sinon.
 */
data class ReferenceItem(
    val id: String,
    val name: String,
    val createdAt: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competenceIds: List<String> = emptyList(),
    val protectionPoints: Int? = null,
    val description: String? = null,
)
