package eu.ejdr.domain.features.auth.entities

/**
 * Utilisateur authentifié du domaine.
 *
 * Conteneur de données pur (aucune méthode métier) : représente l'identité
 * minimale manipulée par les use cases une fois l'authentification réussie.
 *
 * @property id Identifiant unique stable de l'utilisateur.
 * @property email Adresse email servant d'identifiant de connexion.
 * @property pseudo Nom d'affichage de l'utilisateur.
 */
data class User(
    val id: String,
    val email: String,
    val pseudo: String,
)
