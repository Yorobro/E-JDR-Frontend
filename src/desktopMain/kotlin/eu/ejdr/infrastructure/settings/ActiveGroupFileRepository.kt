package eu.ejdr.infrastructure.settings

import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveGroupFileRepository(dataDir: File) : ActiveGroupRepository {

    private val file = File(dataDir, "settings.properties")

    override suspend fun getActiveGroupId(): String? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching {
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("activeGroupId")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    override suspend fun setActiveGroupId(id: String?) = withContext(Dispatchers.IO) {
        runCatching {
            val props = Properties()
            if (file.exists()) file.inputStream().use { props.load(it) }
            if (id != null) props.setProperty("activeGroupId", id)
            else props.remove("activeGroupId")
            file.outputStream().use { props.store(it, null) }
        }
        Unit
    }
}
