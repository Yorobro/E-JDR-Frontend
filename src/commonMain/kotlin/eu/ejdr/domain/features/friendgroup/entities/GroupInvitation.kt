package eu.ejdr.domain.features.friendgroup.entities

data class GroupInvitation(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val invitedByPseudo: String,
    val createdAt: String,
)
