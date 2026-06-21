package eu.ejdr.infrastructure.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Implémentation Android de [SessionStorage].
 *
 * Persiste le `refresh_token` dans des [EncryptedSharedPreferences] (chiffrement AES-256-GCM
 * adossé à une clé du **Android Keystore**), équivalent mobile du couple JCEKS + DPAPI desktop.
 * L'`access_token` reste en mémoire (jamais persisté), conformément au modèle de sécurité commun.
 */
class AndroidSessionStorage(context: Context) : SessionStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ejdr_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val key = "refresh_token"

    override fun load(): String? = prefs.getString(key, null)

    override fun save(value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun clear() {
        prefs.edit().remove(key).apply()
    }

    override fun exists(): Boolean = prefs.contains(key)
}
