package eu.ejdr.infrastructure.http.features.friendgroup.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendGroupDto(
    val id: String,
    val name: String,
    // Tolérant : si la réponse omet myRole (anciens contrats), on retombe sur MEMBER plutôt que
    // de lever une MissingFieldException qui serait alors masquée en « erreur réseau ».
    val myRole: String = "MEMBER",
    val createdAt: String,
)

@Serializable
data class GroupMemberDto(
    val userId: String,
    // Tolérant : défaut vide si la réponse omet le pseudo, plutôt que de lever et masquer en erreur réseau.
    val pseudo: String = "",
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
    // Tolérant : défaut vide si la réponse omet le pseudo de l'invitant.
    val invitedByPseudo: String = "",
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
