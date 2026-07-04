package eu.ejdr.application.features.charactersheet.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/** Use case : liste les fiches du groupe actif (visibilité « tout le groupe »). */
fun interface ListCharacterSheetsUseCase {
    suspend operator fun invoke(groupId: String): Result<List<CharacterSheet>, CharacterSheetError>
}

/**
 * Use case : crée une fiche dans le groupe actif (propriétaire = utilisateur courant), rattachée à
 * une campagne (statut PENDING en attente de validation du MJ).
 */
fun interface CreateCharacterSheetUseCase {
    suspend operator fun invoke(
        name: String,
        groupId: String,
        campaignId: String,
    ): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : récupère le détail complet d'une fiche par son identifiant. */
fun interface GetCharacterSheetUseCase {
    suspend operator fun invoke(id: String): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : met à jour une fiche (nom + champs détaillés). */
fun interface UpdateCharacterSheetUseCase {
    suspend operator fun invoke(sheet: CharacterSheet): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : supprime une fiche de l'utilisateur courant. */
fun interface DeleteCharacterSheetUseCase {
    suspend operator fun invoke(id: String): Result<Unit, CharacterSheetError>
}

/** Use case : liste les fiches ACCEPTÉES rattachées à une campagne. */
fun interface ListCampaignCharactersUseCase {
    suspend operator fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError>
}

/** Use case : liste les demandes de rattachement en attente d'une campagne (MJ uniquement). */
fun interface ListPendingCharactersUseCase {
    suspend operator fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError>
}

/** Use case : valide (MJ) une demande de rattachement en attente. */
fun interface AcceptCharacterUseCase {
    suspend operator fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>
}

/** Use case : refuse (MJ) une demande de rattachement (la fiche est supprimée côté serveur). */
fun interface RefuseCharacterUseCase {
    suspend operator fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>
}

/** Use case : copie une fiche vers une autre campagne (nouvelle fiche PENDING). */
fun interface CopyCharacterSheetUseCase {
    suspend operator fun invoke(
        sheetId: String,
        targetCampaignId: String,
    ): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : liste les campagnes d'une fiche (lecture seule, avec pseudo MJ). */
fun interface GetSheetCampaignsUseCase {
    suspend operator fun invoke(id: String): Result<List<SheetCampaign>, CharacterSheetError>
}

/** Use case : récupère le PDF (binaire) de la fiche courante. */
fun interface ExportCharacterSheetPdfUseCase {
    suspend operator fun invoke(id: String): Result<ByteArray, CharacterSheetError>
}
