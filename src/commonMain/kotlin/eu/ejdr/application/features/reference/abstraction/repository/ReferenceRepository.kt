package eu.ejdr.application.features.reference.abstraction.repository

import eu.ejdr.application.features.reference.abstraction.ReferenceItemForm
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError

/**
 * Port d'accès aux éléments de référence : catalogue (CRUD) et liaison N‑N aux fiches.
 *
 * Implémenté par la couche infrastructure (HTTP) ; consommé par les use cases sans dépendre des
 * détails techniques. Le [ReferenceType] détermine la catégorie ciblée (l'adaptateur HTTP utilise
 * son `slug`). Toutes les opérations renvoient un [Result] : aucune exception ne doit remonter.
 */
interface ReferenceRepository {
    /** Liste les éléments du catalogue du groupe actif pour le type donné. */
    suspend fun list(
        type: ReferenceType,
        groupId: String,
    ): Result<List<ReferenceItem>, ReferenceError>

    /**
     * Crée un élément dans le catalogue du type donné, pour le groupe actif (admin requis).
     *
     * Les champs du [form] ne concernent qu'une partie des types (cf. [ReferenceItemForm]) ; les
     * autres sont ignorés par le serveur.
     */
    suspend fun create(
        type: ReferenceType,
        groupId: String,
        form: ReferenceItemForm,
    ): Result<ReferenceItem, ReferenceError>

    /**
     * Modifie un élément du catalogue (remplacement **complet**, admin requis). Mêmes règles de
     * champs par type que [create] ; le serveur renvoie l'élément modifié.
     */
    suspend fun update(
        type: ReferenceType,
        itemId: String,
        groupId: String,
        form: ReferenceItemForm,
    ): Result<ReferenceItem, ReferenceError>

    /** Supprime un élément du catalogue. */
    suspend fun delete(type: ReferenceType, itemId: String): Result<Unit, ReferenceError>

    /** Liste les éléments (du type liable donné) rattachés à une fiche. */
    suspend fun listLinked(
        sheetId: String,
        type: ReferenceType,
    ): Result<List<ReferenceItem>, ReferenceError>

    /** Rattache un élément à une fiche (N‑N). */
    suspend fun link(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError>

    /** Détache un élément d'une fiche (N‑N). */
    suspend fun unlink(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError>
}
