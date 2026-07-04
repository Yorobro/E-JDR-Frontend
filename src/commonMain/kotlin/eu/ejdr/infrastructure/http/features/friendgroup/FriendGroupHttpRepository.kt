package eu.ejdr.infrastructure.http.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.repository.FriendGroupRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.ChangeMemberRoleRequestDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.CreateGroupRequestDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.FriendGroupDetailDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.FriendGroupDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.GroupListResponseDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.InvitationListResponseDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.InviteMemberRequestDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.InviteResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class FriendGroupHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
) : FriendGroupRepository {

    override suspend fun listMine(): Result<List<FriendGroup>, FriendGroupError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/groups")
            if (response.status.isSuccess()) {
                Result.Success(response.body<GroupListResponseDto>().groups.map(FriendGroupHttpMapper::toGroup))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun get(groupId: String): Result<FriendGroupDetail, FriendGroupError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/groups/$groupId")
            if (response.status.isSuccess()) {
                Result.Success(FriendGroupHttpMapper.toGroupDetail(response.body<FriendGroupDetailDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun create(name: String): Result<FriendGroup, FriendGroupError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/groups") {
                contentType(ContentType.Application.Json)
                setBody(CreateGroupRequestDto(name))
            }
            if (response.status.isSuccess()) {
                Result.Success(FriendGroupHttpMapper.toGroup(response.body<FriendGroupDto>()))
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun delete(groupId: String): Result<Unit, FriendGroupError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/groups/$groupId")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun invite(groupId: String, email: String): Result<String, FriendGroupError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/groups/$groupId/invitations") {
                contentType(ContentType.Application.Json)
                setBody(InviteMemberRequestDto(email))
            }
            if (response.status.isSuccess()) {
                Result.Success(response.body<InviteResponseDto>().invitationId)
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun listMyInvitations(): Result<List<GroupInvitation>, FriendGroupError> =
        runCatchingCancellable {
            val response = client.get("${config.baseUrl}/invitations")
            if (response.status.isSuccess()) {
                Result.Success(
                    response.body<InvitationListResponseDto>().invitations.map(FriendGroupHttpMapper::toInvitation),
                )
            } else {
                failure(response)
            }
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit, FriendGroupError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/invitations/$invitationId/accept")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun declineInvitation(invitationId: String): Result<Unit, FriendGroupError> =
        runCatchingCancellable {
            val response = client.post("${config.baseUrl}/invitations/$invitationId/decline")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit, FriendGroupError> =
        runCatchingCancellable {
            val response = client.delete("${config.baseUrl}/groups/$groupId/members/$userId")
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    override suspend fun changeMemberRole(
        groupId: String,
        userId: String,
        role: String,
    ): Result<Unit, FriendGroupError> =
        runCatchingCancellable {
            val response = client.patch("${config.baseUrl}/groups/$groupId/members/$userId") {
                contentType(ContentType.Application.Json)
                setBody(ChangeMemberRoleRequestDto(role))
            }
            if (response.status.isSuccess()) Result.Success(Unit) else failure(response)
        }.getOrElse { Result.Failure(FriendGroupError.Network) }

    private suspend fun failure(response: HttpResponse): Result.Failure<FriendGroupError> {
        val err = runCatchingCancellable { response.body<ApiErrorDto>() }.getOrNull()
        return Result.Failure(FriendGroupHttpMapper.toError(response.status, err?.code, err?.message))
    }
}
