package eu.ejdr.application.features.friendgroup.abstraction.repository

interface ActiveGroupRepository {
    suspend fun getActiveGroupId(): String?
    suspend fun setActiveGroupId(id: String?)
}
