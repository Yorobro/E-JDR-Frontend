package eu.ejdr.application.features.charactersheet.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/**
 * Port d'accès aux fiches de personnage et à leur liaison aux campagnes.
 *
 * Implémenté par la couche infrastructure (HTTP) ; consommé par les use cases. Toutes les
 * opérations renvoient un [Result] : aucune exception ne doit remonter.
 */
interface CharacterSheetRepository {
    /** Liste les fiches de l'utilisateur courant. */
    suspend fun list(): Result<List<CharacterSheet>, CharacterSheetError>

    /** Crée une fiche appartenant à l'utilisateur courant. */
    suspend fun create(name: String): Result<CharacterSheet, CharacterSheetError>

    /** Récupère le détail complet d'une fiche par son identifiant. */
    suspend fun getById(id: String): Result<CharacterSheet, CharacterSheetError>

    /** Met à jour une fiche (nom + champs détaillés) et renvoie la version persistée. */
    suspend fun update(sheet: CharacterSheet): Result<CharacterSheet, CharacterSheetError>

    /** Supprime une fiche de l'utilisateur courant. */
    suspend fun delete(id: String): Result<Unit, CharacterSheetError>

    /** Liste les fiches rattachées à une campagne. */
    suspend fun listForCampaign(campaignId: String): Result<List<CharacterSheet>, CharacterSheetError>

    /** Liste les fiches rattachables à une campagne (toutes sauf celles du MJ, hors déjà liées). */
    suspend fun listLinkableForCampaign(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError>

    /** Rattache une fiche à une campagne. */
    suspend fun linkToCampaign(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>

    /** Détache une fiche d'une campagne. */
    suspend fun unlinkFromCampaign(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>

    /** Liste les campagnes auxquelles une fiche est rattachée (avec le pseudo du MJ). */
    suspend fun getCampaignsForSheet(id: String): Result<List<SheetCampaign>, CharacterSheetError>

    /** Récupère le PDF (binaire) de la fiche courante sauvegardée. */
    suspend fun exportSheetPdf(id: String): Result<ByteArray, CharacterSheetError>
}
