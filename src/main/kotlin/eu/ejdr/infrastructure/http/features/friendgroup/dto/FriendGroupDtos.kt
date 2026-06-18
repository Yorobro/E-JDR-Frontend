package eu.ejdr.infrastructure.http.features.friendgroup.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendGroupDto(
    val id: String,
    val name: String,
    val myRole: String,
    val createdAt: String,
)

@Serializable
data class GroupMemberDto(
    val userId: String,
    val role: String,
    val createdAt: String,
)

@Serializable
data class FriendGroupDetailDto(
    val id: String,
    val name: String,
    val myRole: String,
    val createdAt: String,
    val members: List<GroupMemberDto>,
)

@Serializable
data class GroupInvitationDto(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val createdAt: String,
)

@Serializable
data class GroupListResponseDto(val groups: List<FriendGroupDto>)

@Serializable
data class InvitationListResponseDto(val invitations: List<GroupInvitationDto>)

@Serializable
data class CreateGroupRequestDto(val name: String)

@Serializable
data class InviteMemberRequestDto(val email: String)

@Serializable
data class InviteResponseDto(val invitationId: String)

@Serializable
data class ChangeMemberRoleRequestDto(val role: String)
