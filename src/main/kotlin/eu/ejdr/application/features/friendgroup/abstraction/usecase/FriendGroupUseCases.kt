package eu.ejdr.application.features.friendgroup.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError

fun interface ListMyGroupsUseCase {
    suspend operator fun invoke(): Result<List<FriendGroup>, FriendGroupError>
}

fun interface GetGroupUseCase {
    suspend operator fun invoke(groupId: String): Result<FriendGroupDetail, FriendGroupError>
}

fun interface CreateGroupUseCase {
    suspend operator fun invoke(name: String): Result<FriendGroup, FriendGroupError>
}

fun interface DeleteGroupUseCase {
    suspend operator fun invoke(groupId: String): Result<Unit, FriendGroupError>
}

fun interface InviteMemberUseCase {
    suspend operator fun invoke(groupId: String, email: String): Result<String, FriendGroupError>
}

fun interface ListMyInvitationsUseCase {
    suspend operator fun invoke(): Result<List<GroupInvitation>, FriendGroupError>
}

fun interface AcceptInvitationUseCase {
    suspend operator fun invoke(invitationId: String): Result<Unit, FriendGroupError>
}

fun interface DeclineInvitationUseCase {
    suspend operator fun invoke(invitationId: String): Result<Unit, FriendGroupError>
}

fun interface RemoveMemberUseCase {
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit, FriendGroupError>
}

fun interface ChangeMemberRoleUseCase {
    suspend operator fun invoke(groupId: String, userId: String, role: String): Result<Unit, FriendGroupError>
}
