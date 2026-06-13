package eu.ejdr.domain.features.charactersheet.entities

/**
 * Fiche de personnage appartenant à un utilisateur.
 *
 * Conteneur de données pur (domaine front anémique) : représente une fiche telle que reçue
 * du serveur. Aucune logique métier — les règles vivent côté backend.
 *
 * @property id Identifiant unique stable de la fiche.
 * @property ownerId Identifiant du propriétaire de la fiche.
 * @property name Nom affiché de la fiche.
 * @property createdAt Date de création au format ISO 8601 (telle que renvoyée par l'API).
 */
data class CharacterSheet(
    val id: String,
    val ownerId: String,
    val name: String,
    val createdAt: String,
)
