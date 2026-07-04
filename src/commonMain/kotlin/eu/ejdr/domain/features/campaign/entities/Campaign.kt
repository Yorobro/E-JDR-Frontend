package eu.ejdr.domain.features.campaign.entities

/**
 * Campagne de jeu de rôle dont l'utilisateur courant est le maître du jeu.
 *
 * Conteneur de données pur (domaine front anémique) : représente une campagne telle
 * que reçue du serveur. Aucune logique métier — les règles vivent côté backend.
 *
 * @property id Identifiant unique stable de la campagne.
 * @property name Nom affiché de la campagne.
 * @property gameMasterId Identifiant du maître du jeu (propriétaire de la campagne).
 * @property createdAt Date de création au format ISO 8601 (telle que renvoyée par l'API).
 */
data class Campaign(
    val id: String,
    val name: String,
    val gameMasterId: String,
    val createdAt: String,
)
