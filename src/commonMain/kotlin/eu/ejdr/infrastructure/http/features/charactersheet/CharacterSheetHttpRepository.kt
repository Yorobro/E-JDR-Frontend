package eu.ejdr.infrastructure.http.features.charactersheet

import eu.ejdr.application.features.charactersheet.abstraction.repository.CharacterSheetRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CampaignCharactersResponseDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CharacterSheetDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CharacterSheetListResponseDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CopyCharacterSheetRequestDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CreateCharacterSheetRequestDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.SheetCampaignsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implémentation HTTP de [CharacterSheetRepository] : routes `/character-sheets` (CRUD) et
 * `/campaigns/{id}/characters` (liaison). Modèle identique à `AuthHttpRepository` /
 * `CampaignHttpRepository` (cookies HttpOnly transparents, refresh-on-401).
 *
 * Toute erreur de transport devient [CharacterSheetError.Network] ; les échecs applicatifs
 * passent par [CharacterSheetHttpMapper.toError].
 *
 * @property client Client HTTP configuré (cookies + JSON).
 * @property config Configuration applicative (URL de base).
 */
class CharacterSheetHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
) : CharacterSheetRepository {

    override suspend fun list(groupId: String): Result<List<CharacterSheet>, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/character-sheets") {
                parameter("groupId", groupId)
            }
            if (response.status.isSuccess()) {
                val body = response.body<CharacterSheetListResponseDto>()
                Result.Success(body.characterSheets.map(CharacterSheetHttpMapper::toCharacterSheet))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun create(
        name: String,
        groupId: String,
        campaignId: String,
    ): Result<CharacterSheet, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/character-sheets") {
                contentType(ContentType.Application.Json)
                setBody(CreateCharacterSheetRequestDto(name, groupId, campaignId))
            }
            if (response.status.isSuccess()) {
                Result.Success(CharacterSheetHttpMapper.toCharacterSheet(response.body<CharacterSheetDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun getById(id: String): Result<CharacterSheet, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/character-sheets/$id")
            if (response.status.isSuccess()) {
                Result.Success(CharacterSheetHttpMapper.toCharacterSheet(response.body<CharacterSheetDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun update(
        sheet: CharacterSheet,
    ): Result<CharacterSheet, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.put("${config.baseUrl}/character-sheets/${sheet.id}") {
                contentType(ContentType.Application.Json)
                setBody(CharacterSheetHttpMapper.toUpdateRequest(sheet))
            }
            if (response.status.isSuccess()) {
                Result.Success(CharacterSheetHttpMapper.toCharacterSheet(response.body<CharacterSheetDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun delete(id: String): Result<Unit, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/character-sheets/$id")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun listForCampaign(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/campaigns/$campaignId/characters")
            if (response.status.isSuccess()) {
                val body = response.body<CampaignCharactersResponseDto>()
                Result.Success(body.characters.map(CharacterSheetHttpMapper::toCharacterSheet))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun listPendingForCampaign(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> =
        runCatchingCancellable {
            val response =
                client.get("${config.baseUrl}/campaigns/$campaignId/pending-characters")
            if (response.status.isSuccess()) {
                val body = response.body<CampaignCharactersResponseDto>()
                Result.Success(body.characters.map(CharacterSheetHttpMapper::toCharacterSheet))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun acceptCharacter(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.post(
                "${config.baseUrl}/campaigns/$campaignId/characters/$characterSheetId/accept",
            )
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun refuseCharacter(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.post(
                "${config.baseUrl}/campaigns/$campaignId/characters/$characterSheetId/refuse",
            )
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun copyToCampaign(
        sheetId: String,
        targetCampaignId: String,
    ): Result<CharacterSheet, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/character-sheets/$sheetId/copy") {
                contentType(ContentType.Application.Json)
                setBody(CopyCharacterSheetRequestDto(targetCampaignId))
            }
            if (response.status.isSuccess()) {
                Result.Success(CharacterSheetHttpMapper.toCharacterSheet(response.body<CharacterSheetDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun getCampaignsForSheet(
        id: String,
    ): Result<List<SheetCampaign>, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/character-sheets/$id/campaigns")
            if (response.status.isSuccess()) {
                val body = response.body<SheetCampaignsResponseDto>()
                Result.Success(body.campaigns.map(CharacterSheetHttpMapper::toSheetCampaign))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    override suspend fun exportSheetPdf(id: String): Result<ByteArray, CharacterSheetError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/character-sheets/$id/export-pdf")
            if (response.status.isSuccess()) {
                Result.Success(response.readRawBytes())
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(CharacterSheetError.Network) }

    /** Lit le corps d'erreur (best-effort) et le traduit via le mapper. */
    private suspend fun failure(response: HttpResponse): Result.Failure<CharacterSheetError> {
        val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
        return Result.Failure(
            CharacterSheetHttpMapper.toError(response.status, err?.code, err?.message),
        )
    }
}
