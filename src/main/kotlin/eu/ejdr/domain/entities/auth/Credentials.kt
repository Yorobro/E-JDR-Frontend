package eu.ejdr.domain.entities.auth

/**
 * Identifiants de connexion fournis par l'utilisateur.
 *
 * Conteneur de données pur (aucune méthode métier) : transporte les informations
 * saisies vers les use cases d'authentification (connexion, inscription).
 *
 * @property email Adresse email saisie.
 * @property password Mot de passe en clair, le temps de la requête d'authentification.
 */
data class Credentials(
    val email: String,
    val password: String,
)
