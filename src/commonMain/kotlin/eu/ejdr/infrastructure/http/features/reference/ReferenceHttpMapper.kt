package eu.ejdr.infrastructure.http.features.reference

import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.error.ReferenceError
import eu.ejdr.infrastructure.http.features.reference.dto.ReferenceItemDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut HTTP) de la feature référence vers le
 * domaine. Frontière isolant la couche application des détails du protocole. Sans état.
 */
object ReferenceHttpMapper {

    /**
     * Convertit un élément reçu de l'API en entité domaine.
     *
     * @param dto Élément JSON désérialisé.
     * @return L'[ReferenceItem] correspondant.
     */
    fun toItem(dto: ReferenceItemDto): ReferenceItem =
        ReferenceItem(
            id = dto.id,
            name = dto.name,
            createdAt = dto.createdAt,
            stat = dto.stat,
            bonus = dto.bonus,
            competenceIds = dto.competenceIds,
            protectionPoints = dto.protectionPoints,
            description = dto.description,
        )

    /**
     * Traduit un échec HTTP en erreur métier référence.
     *
     * Le **code applicatif** prime quand il est présent (contrat partagé avec le backend) ;
     * à défaut on retombe sur le statut HTTP, puis sur [ReferenceError.Unknown].
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel.
     * @param message Message d'erreur lisible éventuel.
     * @return La [ReferenceError] du domaine correspondante.
     */
    fun toError(status: HttpStatusCode, code: String?, message: String?): ReferenceError =
        when (code) {
            "INVALID_REFERENCE_NAME" -> ReferenceError.InvalidName
            "REFERENCE_NAME_ALREADY_USED" -> ReferenceError.NameAlreadyUsed
            "REFERENCE_ITEM_NOT_FOUND", "CHARACTER_SHEET_NOT_FOUND" -> ReferenceError.NotFound
            "CHARACTER_SHEET_ACCESS_DENIED" -> ReferenceError.AccessDenied
            else -> when (status) {
                HttpStatusCode.NotFound -> ReferenceError.NotFound
                HttpStatusCode.Conflict -> ReferenceError.NameAlreadyUsed
                HttpStatusCode.Forbidden -> ReferenceError.AccessDenied
                HttpStatusCode.BadRequest -> ReferenceError.InvalidName
                else -> ReferenceError.Unknown(message ?: code ?: status.description)
            }
        }
}
