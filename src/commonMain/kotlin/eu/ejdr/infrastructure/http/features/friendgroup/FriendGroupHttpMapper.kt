package eu.ejdr.infrastructure.http.features.friendgroup

import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError
import eu.ejdr.infrastructure.http.features.friendgroup.dto.FriendGroupDetailDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.FriendGroupDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.GroupInvitationDto
import eu.ejdr.infrastructure.http.features.friendgroup.dto.GroupMemberDto
import io.ktor.http.HttpStatusCode

object FriendGroupHttpMapper {

    fun toGroup(dto: FriendGroupDto): FriendGroup =
        FriendGroup(id = dto.id, name = dto.name, myRole = dto.myRole, createdAt = dto.createdAt)

    fun toGroupDetail(dto: FriendGroupDetailDto): FriendGroupDetail =
        FriendGroupDetail(
            id = dto.id,
            name = dto.name,
            myRole = dto.myRole,
            createdAt = dto.createdAt,
            members = dto.members.map(::toMember),
        )

    fun toMember(dto: GroupMemberDto): GroupMember =
        GroupMember(userId = dto.userId, pseudo = dto.pseudo, role = dto.role, createdAt = dto.createdAt)

    fun toInvitation(dto: GroupInvitationDto): GroupInvitation =
        GroupInvitation(
            id = dto.id,
            groupId = dto.groupId,
            groupName = dto.groupName,
            invitedBy = dto.invitedBy,
            invitedByPseudo = dto.invitedByPseudo,
            createdAt = dto.createdAt,
        )

    fun toError(status: HttpStatusCode, code: String?, message: String?): FriendGroupError =
        when (code) {
            "GROUP_NOT_FOUND" -> FriendGroupError.NotFound
            "NOT_GROUP_MEMBER" -> FriendGroupError.NotMember
            "NOT_GROUP_ADMIN" -> FriendGroupError.NotAdmin
            "INVITATION_NOT_FOUND" -> FriendGroupError.InvitationNotFound
            "INVITATION_ALREADY_RESOLVED" -> FriendGroupError.InvitationAlreadyResolved
            "ALREADY_MEMBER" -> FriendGroupError.AlreadyMember
            "INVITATION_ALREADY_PENDING" -> FriendGroupError.InvitationAlreadyPending
            "INVITED_USER_NOT_FOUND" -> FriendGroupError.InvitedUserNotFound
            "CANNOT_REMOVE_LAST_ADMIN" -> FriendGroupError.CannotRemoveLastAdmin
            "CANNOT_REMOVE_ADMIN" -> FriendGroupError.CannotRemoveAdmin
            "INVALID_GROUP_NAME" -> FriendGroupError.InvalidGroupName
            else -> when (status) {
                HttpStatusCode.NotFound -> FriendGroupError.NotFound
                HttpStatusCode.Forbidden -> FriendGroupError.NotMember
                HttpStatusCode.Conflict -> FriendGroupError.AlreadyMember
                HttpStatusCode.BadRequest -> FriendGroupError.InvalidGroupName
                else -> FriendGroupError.Unknown(message ?: code ?: status.description)
            }
        }
}
