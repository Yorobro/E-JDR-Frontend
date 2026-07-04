package eu.ejdr.domain.features.friendgroup.entities

data class FriendGroupDetail(
    val id: String,
    val name: String,
    val myRole: String,
    val createdAt: String,
    val members: List<GroupMember>,
)
