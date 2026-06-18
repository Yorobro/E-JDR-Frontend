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
 */
data class ReferenceItem(
    val id: String,
    val name: String,
    val createdAt: String,
)
