package eu.ejdr.domain.features.friendgroup.entities

data class GroupMember(
    val userId: String,
    val pseudo: String,
    val role: String,
    val createdAt: String,
)
