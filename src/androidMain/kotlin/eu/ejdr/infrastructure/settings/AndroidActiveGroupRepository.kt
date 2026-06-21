package eu.ejdr.infrastructure.settings

import android.content.Context
import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Implémentation Android de [ActiveGroupRepository] : persiste l'id du groupe actif en SharedPreferences. */
class AndroidActiveGroupRepository(context: Context) : ActiveGroupRepository {

    private val prefs = context.getSharedPreferences("ejdr_settings", Context.MODE_PRIVATE)
    private val key = "activeGroupId"

    override suspend fun getActiveGroupId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    override suspend fun setActiveGroupId(id: String?): Unit = withContext(Dispatchers.IO) {
        if (id != null) prefs.edit().putString(key, id).apply()
        else prefs.edit().remove(key).apply()
    }
}
