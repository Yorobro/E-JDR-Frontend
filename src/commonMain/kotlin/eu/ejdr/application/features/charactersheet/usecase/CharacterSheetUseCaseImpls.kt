package eu.ejdr.application.features.charactersheet.usecase

import eu.ejdr.application.features.charactersheet.abstraction.repository.CharacterSheetRepository
import eu.ejdr.application.features.charactersheet.abstraction.usecase.AcceptCharacterUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CopyCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListPendingCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.RefuseCharacterUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/** Implémentations triviales déléguant au [CharacterSheetRepository]. */

class ListCharacterSheetsUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListCharacterSheetsUseCase {
    override suspend fun invoke(groupId: String): Result<List<CharacterSheet>, CharacterSheetError> =
        repository.list(groupId)
}

class CreateCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : CreateCharacterSheetUseCase {
    override suspend fun invoke(
        name: String,
        groupId: String,
        campaignId: String,
    ): Result<CharacterSheet, CharacterSheetError> = repository.create(name, groupId, campaignId)
}

class GetCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : GetCharacterSheetUseCase {
    override suspend fun invoke(id: String): Result<CharacterSheet, CharacterSheetError> =
        repository.getById(id)
}

class UpdateCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : UpdateCharacterSheetUseCase {
    override suspend fun invoke(
        sheet: CharacterSheet,
    ): Result<CharacterSheet, CharacterSheetError> = repository.update(sheet)
}

class DeleteCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : DeleteCharacterSheetUseCase {
    override suspend fun invoke(id: String): Result<Unit, CharacterSheetError> =
        repository.delete(id)
}

class ListCampaignCharactersUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListCampaignCharactersUseCase {
    override suspend fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> = repository.listForCampaign(campaignId)
}

class ListPendingCharactersUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListPendingCharactersUseCase {
    override suspend fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> =
        repository.listPendingForCampaign(campaignId)
}

class AcceptCharacterUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : AcceptCharacterUseCase {
    override suspend fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> = repository.acceptCharacter(campaignId, characterSheetId)
}

class RefuseCharacterUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : RefuseCharacterUseCase {
    override suspend fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> = repository.refuseCharacter(campaignId, characterSheetId)
}

class CopyCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : CopyCharacterSheetUseCase {
    override suspend fun invoke(
        sheetId: String,
        targetCampaignId: String,
    ): Result<CharacterSheet, CharacterSheetError> =
        repository.copyToCampaign(sheetId, targetCampaignId)
}

class GetSheetCampaignsUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : GetSheetCampaignsUseCase {
    override suspend fun invoke(id: String): Result<List<SheetCampaign>, CharacterSheetError> =
        repository.getCampaignsForSheet(id)
}

class ExportCharacterSheetPdfUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ExportCharacterSheetPdfUseCase {
    override suspend fun invoke(id: String): Result<ByteArray, CharacterSheetError> =
        repository.exportSheetPdf(id)
}
