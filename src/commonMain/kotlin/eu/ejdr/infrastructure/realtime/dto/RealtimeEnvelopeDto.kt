package eu.ejdr.infrastructure.realtime.dto

import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import kotlinx.serialization.Serializable

/**
 * Enveloppe de transport d'un message temps réel (contrat WebSocket).
 *
 * Traduite vers/depuis l'enveloppe domaine [RealtimeMessage]. Reste minimale (type +
 * payload JSON brut) tant que le protocole serveur des features temps réel n'est pas
 * défini.
 *
 * @property type Discriminant applicatif du message.
 * @property payload Charge utile JSON brute.
 */
@Serializable
data class RealtimeEnvelopeDto(
    val type: String,
    val payload: String,
)

internal fun RealtimeEnvelopeDto.toDomain(): RealtimeMessage =
    RealtimeMessage(type = type, payload = payload)

internal fun RealtimeMessage.toDto(): RealtimeEnvelopeDto =
    RealtimeEnvelopeDto(type = type, payload = payload)
