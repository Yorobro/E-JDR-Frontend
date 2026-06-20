package eu.ejdr.application.features.friendgroup.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError

interface FriendGroupRepository {
    suspend fun listMine(): Result<List<FriendGroup>, FriendGroupError>
    suspend fun get(groupId: String): Result<FriendGroupDetail, FriendGroupError>
    suspend fun create(name: String): Result<FriendGroup, FriendGroupError>
    suspend fun delete(groupId: String): Result<Unit, FriendGroupError>
    suspend fun invite(groupId: String, email: String): Result<String, FriendGroupError>
    suspend fun listMyInvitations(): Result<List<GroupInvitation>, FriendGroupError>
    suspend fun acceptInvitation(invitationId: String): Result<Unit, FriendGroupError>
    suspend fun declineInvitation(invitationId: String): Result<Unit, FriendGroupError>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit, FriendGroupError>
    suspend fun changeMemberRole(groupId: String, userId: String, role: String): Result<Unit, FriendGroupError>
}
