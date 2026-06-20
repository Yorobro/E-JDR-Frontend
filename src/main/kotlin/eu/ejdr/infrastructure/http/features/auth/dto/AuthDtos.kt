package eu.ejdr.infrastructure.http.features.auth.dto

import kotlinx.serialization.Serializable

/**
 * Corps de requête envoyé au serveur pour l'authentification (login/register).
 *
 * Contrat de transport HTTP : il sérialise les identifiants vers le format JSON
 * attendu par l'API et ne doit pas être confondu avec l'entité domaine
 * [eu.ejdr.domain.features.auth.entities.Credentials].
 *
 * @property email Adresse e-mail de l'utilisateur.
 * @property password Mot de passe en clair, transmis uniquement via HTTPS.
 */
@Serializable
data class AuthRequestDto(val email: String, val password: String)

/** Corps de requête d'inscription (`/auth/register`) : ajoute le pseudo (nom d'affichage). */
@Serializable
data class RegisterRequestDto(val email: String, val password: String, val pseudo: String)

/**
 * Corps de réponse renvoyé par le serveur en cas d'authentification réussie.
 *
 * Contrat de transport HTTP : il décrit la forme JSON reçue, traduite ensuite
 * vers l'entité domaine [eu.ejdr.domain.features.auth.entities.User] par [AuthHttpMapper].
 * Les jetons ne figurent pas ici : ils sont posés par le serveur sous forme de
 * cookies HttpOnly.
 *
 * @property userId Identifiant unique de l'utilisateur authentifié.
 * @property email Adresse e-mail de l'utilisateur authentifié.
 */
@Serializable
data class AuthResponseDto(val userId: String, val email: String)

/**
 * Corps d'erreur renvoyé par l'API lorsqu'une requête échoue.
 *
 * Contrat de transport HTTP : il est combiné au statut HTTP par [AuthHttpMapper]
 * pour produire une [eu.ejdr.domain.features.auth.error.AuthError] du domaine.
 * Les deux champs sont optionnels, le serveur ne fournissant pas toujours un
 * détail structuré.
 *
 * @property code Code d'erreur applicatif éventuel.
 * @property message Message d'erreur lisible éventuel.
 */
@Serializable
data class ApiErrorDto(val code: String? = null, val message: String? = null)

/** Corps de requête envoyé au serveur pour changer l'adresse e-mail (`PATCH /me/email`). */
@Serializable
data class ChangeEmailRequestDto(val email: String)

/** Corps de requête envoyé au serveur pour changer le mot de passe (`PATCH /me/password`). */
@Serializable
data class ChangePasswordRequestDto(val currentPassword: String, val newPassword: String)
