package eu.ejdr.application.features.friendgroup.usecase

import eu.ejdr.application.features.friendgroup.abstraction.repository.FriendGroupRepository
import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.CreateGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeleteGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyGroupsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError

class ListMyGroupsUseCaseImpl(private val repository: FriendGroupRepository) : ListMyGroupsUseCase {
    override suspend fun invoke(): Result<List<FriendGroup>, FriendGroupError> = repository.listMine()
}

class GetGroupUseCaseImpl(private val repository: FriendGroupRepository) : GetGroupUseCase {
    override suspend fun invoke(groupId: String): Result<FriendGroupDetail, FriendGroupError> =
        repository.get(groupId)
}

class CreateGroupUseCaseImpl(private val repository: FriendGroupRepository) : CreateGroupUseCase {
    override suspend fun invoke(name: String): Result<FriendGroup, FriendGroupError> =
        repository.create(name)
}

class DeleteGroupUseCaseImpl(private val repository: FriendGroupRepository) : DeleteGroupUseCase {
    override suspend fun invoke(groupId: String): Result<Unit, FriendGroupError> =
        repository.delete(groupId)
}

class InviteMemberUseCaseImpl(private val repository: FriendGroupRepository) : InviteMemberUseCase {
    override suspend fun invoke(groupId: String, email: String): Result<String, FriendGroupError> =
        repository.invite(groupId, email)
}

class ListMyInvitationsUseCaseImpl(private val repository: FriendGroupRepository) : ListMyInvitationsUseCase {
    override suspend fun invoke(): Result<List<GroupInvitation>, FriendGroupError> =
        repository.listMyInvitations()
}

class AcceptInvitationUseCaseImpl(private val repository: FriendGroupRepository) : AcceptInvitationUseCase {
    override suspend fun invoke(invitationId: String): Result<Unit, FriendGroupError> =
        repository.acceptInvitation(invitationId)
}

class DeclineInvitationUseCaseImpl(private val repository: FriendGroupRepository) : DeclineInvitationUseCase {
    override suspend fun invoke(invitationId: String): Result<Unit, FriendGroupError> =
        repository.declineInvitation(invitationId)
}

class RemoveMemberUseCaseImpl(private val repository: FriendGroupRepository) : RemoveMemberUseCase {
    override suspend fun invoke(groupId: String, userId: String): Result<Unit, FriendGroupError> =
        repository.removeMember(groupId, userId)
}

class ChangeMemberRoleUseCaseImpl(private val repository: FriendGroupRepository) : ChangeMemberRoleUseCase {
    override suspend fun invoke(groupId: String, userId: String, role: String): Result<Unit, FriendGroupError> =
        repository.changeMemberRole(groupId, userId, role)
}
