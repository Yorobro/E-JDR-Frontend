package eu.ejdr.infrastructure.realtime.dto

import kotlinx.serialization.Serializable

/**
 * Enveloppe d'un event d'invalidation poussé par le backend sur le WebSocket.
 *
 * Le serveur ne pousse pas le contenu modifié, seulement un signal « tel périmètre a changé » :
 * le client recharge ensuite via REST. Champs à plat (pas de `payload` imbriqué).
 *
 * @property type Discriminant ; vaut `"invalidate"` pour un signal de changement.
 * @property channel Canal de diffusion (`user:{id}` / `group:{id}` / `sheet:{id}`).
 * @property resource Ressource à recharger (ex. `character-sheets`).
 * @property scopeId Identifiant du périmètre (groupe, fiche ou utilisateur).
 */
@Serializable
data class InvalidationDto(
    val type: String,
    val channel: String,
    val resource: String,
    val scopeId: String,
)
