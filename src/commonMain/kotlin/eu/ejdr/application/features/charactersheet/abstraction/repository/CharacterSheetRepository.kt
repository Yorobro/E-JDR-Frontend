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
    /** Liste les fiches du groupe actif (visibilité « tout le groupe »). */
    suspend fun list(groupId: String): Result<List<CharacterSheet>, CharacterSheetError>

    /**
     * Crée une fiche dans le groupe actif (propriétaire = utilisateur courant), rattachée à une
     * campagne (statut PENDING en attente de validation du MJ).
     */
    suspend fun create(
        name: String,
        groupId: String,
        campaignId: String,
    ): Result<CharacterSheet, CharacterSheetError>

    /** Récupère le détail complet d'une fiche par son identifiant. */
    suspend fun getById(id: String): Result<CharacterSheet, CharacterSheetError>

    /** Met à jour une fiche (nom + champs détaillés) et renvoie la version persistée. */
    suspend fun update(sheet: CharacterSheet): Result<CharacterSheet, CharacterSheetError>

    /** Supprime une fiche de l'utilisateur courant. */
    suspend fun delete(id: String): Result<Unit, CharacterSheetError>

    /** Liste les fiches ACCEPTÉES rattachées à une campagne. */
    suspend fun listForCampaign(campaignId: String): Result<List<CharacterSheet>, CharacterSheetError>

    /** Liste les demandes de rattachement en attente d'une campagne (MJ uniquement, côté back). */
    suspend fun listPendingForCampaign(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError>

    /** Valide (MJ) une demande de rattachement en attente. */
    suspend fun acceptCharacter(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>

    /** Refuse (MJ) une demande de rattachement en attente (la fiche est supprimée côté serveur). */
    suspend fun refuseCharacter(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>

    /** Copie une fiche vers une autre campagne (nouvelle fiche PENDING). */
    suspend fun copyToCampaign(
        sheetId: String,
        targetCampaignId: String,
    ): Result<CharacterSheet, CharacterSheetError>

    /** Liste les campagnes auxquelles une fiche est rattachée (avec le pseudo du MJ). */
    suspend fun getCampaignsForSheet(id: String): Result<List<SheetCampaign>, CharacterSheetError>

    /** Récupère le PDF (binaire) de la fiche courante sauvegardée. */
    suspend fun exportSheetPdf(id: String): Result<ByteArray, CharacterSheetError>
}
