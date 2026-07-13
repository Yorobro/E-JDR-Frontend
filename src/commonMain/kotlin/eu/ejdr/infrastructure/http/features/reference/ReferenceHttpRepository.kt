package eu.ejdr.infrastructure.http.features.reference

import eu.ejdr.application.features.reference.abstraction.ReferenceItemForm
import eu.ejdr.application.features.reference.abstraction.repository.ReferenceRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.reference.dto.CreateReferenceRequestDto
import eu.ejdr.infrastructure.http.features.reference.dto.LinkReferenceRequestDto
import eu.ejdr.infrastructure.http.features.reference.dto.ReferenceItemDto
import eu.ejdr.infrastructure.http.features.reference.dto.ReferenceListResponseDto
import eu.ejdr.infrastructure.http.features.reference.dto.StatBonusDto
import eu.ejdr.infrastructure.http.features.reference.dto.UpdateReferenceRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implémentation HTTP de [ReferenceRepository] dialoguant avec les routes `/reference/{slug}`
 * (catalogue) et `/character-sheets/{id}/{slug}` (liaison N‑N).
 *
 * Cookies HttpOnly gérés par le [HttpClient] (refresh-on-401 transparent). Toute erreur de
 * transport devient [ReferenceError.Network] ; les échecs applicatifs passent par
 * [ReferenceHttpMapper.toError].
 *
 * @property client Client HTTP configuré (cookies + JSON).
 * @property config Configuration applicative (URL de base).
 */
class ReferenceHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
) : ReferenceRepository {

    override suspend fun list(
        type: ReferenceType,
        groupId: String,
    ): Result<List<ReferenceItem>, ReferenceError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/reference/${type.slug}") {
                parameter("groupId", groupId)
            }
            if (response.status.isSuccess()) {
                Result.Success(response.body<ReferenceListResponseDto>().items.map(ReferenceHttpMapper::toItem))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    override suspend fun create(
        type: ReferenceType,
        groupId: String,
        form: ReferenceItemForm,
    ): Result<ReferenceItem, ReferenceError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/reference/${type.slug}") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateReferenceRequestDto(
                        name = form.name,
                        groupId = groupId,
                        stat = form.stat,
                        bonus = form.bonus,
                        statBonuses = form.toStatBonusDtos(),
                        competenceIds = form.competenceIds.ifEmpty { null },
                        protectionPoints = form.protectionPoints,
                        description = form.description,
                    ),
                )
            }
            if (response.status.isSuccess()) {
                Result.Success(ReferenceHttpMapper.toItem(response.body<ReferenceItemDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    override suspend fun update(
        type: ReferenceType,
        itemId: String,
        groupId: String,
        form: ReferenceItemForm,
    ): Result<ReferenceItem, ReferenceError> =
        runCatchingCancellable {
            val response = client.put("${config.baseUrl}/reference/${type.slug}/$itemId") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateReferenceRequestDto(
                        name = form.name,
                        groupId = groupId,
                        stat = form.stat,
                        bonus = form.bonus,
                        statBonuses = form.toStatBonusDtos(),
                        competenceIds = form.competenceIds.ifEmpty { null },
                        protectionPoints = form.protectionPoints,
                        description = form.description,
                    ),
                )
            }
            if (response.status.isSuccess()) {
                Result.Success(ReferenceHttpMapper.toItem(response.body<ReferenceItemDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    /**
     * Bonus du peuple en DTO, ou `null` s'il n'y en a aucun — même convention que `competenceIds` :
     * un champ absent plutôt qu'un tableau vide, pour ne pas envoyer de bruit sur les types qui ne
     * portent pas de bonus multiples.
     */
    private fun ReferenceItemForm.toStatBonusDtos(): List<StatBonusDto>? =
        statBonuses.map { StatBonusDto(it.stat, it.bonus) }.ifEmpty { null }

    override suspend fun delete(
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/reference/${type.slug}/$itemId")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    override suspend fun listLinked(
        sheetId: String,
        type: ReferenceType,
    ): Result<List<ReferenceItem>, ReferenceError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/character-sheets/$sheetId/${type.slug}")
            if (response.status.isSuccess()) {
                Result.Success(response.body<ReferenceListResponseDto>().items.map(ReferenceHttpMapper::toItem))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    override suspend fun link(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/character-sheets/$sheetId/${type.slug}") {
                contentType(ContentType.Application.Json)
                setBody(LinkReferenceRequestDto(itemId))
            }
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    override suspend fun unlink(
        sheetId: String,
        type: ReferenceType,
        itemId: String,
    ): Result<Unit, ReferenceError> =
        runCatchingCancellable {
            val response =
                client.delete("${config.baseUrl}/character-sheets/$sheetId/${type.slug}/$itemId")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(ReferenceError.Network) }

    /** Lit le corps d'erreur (best-effort) et le traduit en [ReferenceError] via le mapper. */
    private suspend fun failure(response: HttpResponse): Result.Failure<ReferenceError> {
        val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
        return Result.Failure(ReferenceHttpMapper.toError(response.status, err?.code, err?.message))
    }
}
