package eu.ejdr.infrastructure.http.features.reference.dto

import kotlinx.serialization.Serializable

/**
 * Représentation JSON d'un élément de référence renvoyée par l'API.
 *
 * Les champs [stat], [bonus] et [competenceIds] ne concernent que les formations/peuples ; ils sont
 * `null`/vides pour les autres types et **tolérants** (défauts) pour rester robuste si l'API ne les
 * renvoie pas.
 *
 * @property id Identifiant unique de l'élément.
 * @property name Nom de l'élément.
 * @property createdAt Date de création au format ISO 8601.
 * @property stat Statistique associée (slug serveur), ou `null`.
 * @property bonus Montant du bonus appliqué à la statistique, ou `null`.
 * @property competenceIds Identifiants des compétences liées (formation), vide sinon.
 * @property protectionPoints Points de protection (armure uniquement), ou `null` ; tolérant.
 */
@Serializable
data class ReferenceItemDto(
    val id: String,
    val name: String,
    val createdAt: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competenceIds: List<String> = emptyList(),
    val protectionPoints: Int? = null,
)

/**
 * Corps de requête de création d'un élément (`POST /reference/{type}`).
 *
 * @property name Nom de l'élément.
 * @property groupId Groupe propriétaire.
 * @property stat Statistique associée (formation/peuple), ou `null` ; omise pour les autres types.
 * @property bonus Montant du bonus (le serveur applique `1` par défaut si [stat] est fournie sans).
 * @property competenceIds Compétences à lier (formation), ou `null` pour les autres types.
 * @property protectionPoints Points de protection (armure uniquement), ou `null` pour les autres types.
 */
@Serializable
data class CreateReferenceRequestDto(
    val name: String,
    val groupId: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competenceIds: List<String>? = null,
    val protectionPoints: Int? = null,
)

/**
 * Corps de requête de modification d'un élément (`PUT /reference/{type}/{itemId}`).
 *
 * Remplacement **complet** : tous les champs de l'élément sont transmis (mêmes règles par type que
 * [CreateReferenceRequestDto]). Le serveur renvoie l'élément modifié.
 *
 * @property name Nom de l'élément.
 * @property groupId Groupe propriétaire.
 * @property stat Statistique associée (formation/peuple), ou `null` ; omise pour les autres types.
 * @property bonus Montant du bonus (le serveur applique `1` par défaut si [stat] est fournie sans).
 * @property competenceIds Compétences à lier (formation), ou `null` pour les autres types.
 * @property protectionPoints Points de protection (armure uniquement), ou `null` pour les autres types.
 */
@Serializable
data class UpdateReferenceRequestDto(
    val name: String,
    val groupId: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competenceIds: List<String>? = null,
    val protectionPoints: Int? = null,
)

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
