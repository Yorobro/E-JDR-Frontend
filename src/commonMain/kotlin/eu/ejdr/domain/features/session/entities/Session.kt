package eu.ejdr.domain.features.session.entities

/**
 * Session de jeu rattachée à une campagne (une rencontre du groupe pour jouer).
 *
 * Conteneur de données pur (domaine front anémique) : représente une session telle que reçue
 * du serveur. Aucune logique métier — les règles vivent côté backend.
 *
 * @property id Identifiant unique stable de la session.
 * @property campaignId Identifiant de la campagne parente.
 * @property title Titre affiché de la session.
 * @property date Date de la session au format `YYYY-MM-DD` (telle que renvoyée par l'API).
 * @property createdAt Date de création au format ISO 8601 (telle que renvoyée par l'API).
 */
data class Session(
    val id: String,
    val campaignId: String,
    val title: String,
    val date: String,
    val createdAt: String,
)
