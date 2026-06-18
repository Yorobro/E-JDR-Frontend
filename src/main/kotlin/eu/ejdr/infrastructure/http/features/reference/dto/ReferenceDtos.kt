package eu.ejdr.infrastructure.http.features.reference.dto

import kotlinx.serialization.Serializable

/**
 * Représentation JSON d'un élément de référence renvoyée par l'API.
 *
 * @property id Identifiant unique de l'élément.
 * @property name Nom de l'élément.
 * @property createdAt Date de création au format ISO 8601.
 */
@Serializable
data class ReferenceItemDto(val id: String, val name: String, val createdAt: String)

/** Corps de requête de création d'un élément (`POST /reference/{type}`). */
@Serializable
data class CreateReferenceRequestDto(val name: String, val groupId: String)

/**
 * Réponse de listing (catalogue `GET /reference/{type}` et liaisons `GET /character-sheets/:id/{type}`) :
 * l'API enveloppe la liste sous la clé `items`.
 *
 * @property items Éléments renvoyés.
 */
@Serializable
data class ReferenceListResponseDto(val items: List<ReferenceItemDto>)

/** Corps de requête de rattachement d'un élément à une fiche (`POST /character-sheets/:id/{type}`). */
@Serializable
data class LinkReferenceRequestDto(val itemId: String)
