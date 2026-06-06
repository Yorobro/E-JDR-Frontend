package eu.ejdr.infrastructure.security

import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Gère un KeyStore JCEKS local contenant une clé AES.
 * La clé sert à chiffrer le refresh_token persisté.
 */
class KeyStoreProvider(
    dataDir: File,
    private val storePassword: CharArray = "ejdr-local-store".toCharArray(),
) {
    private val storeFile = File(dataDir, "ejdr.jceks")
    private val alias = "cookie-key"

    fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("JCEKS")
        if (storeFile.exists()) {
            storeFile.inputStream().use { store.load(it, storePassword) }
        } else {
            store.load(null, storePassword)
        }
        val entry = store.getEntry(alias, KeyStore.PasswordProtection(storePassword))
        if (entry is KeyStore.SecretKeyEntry) {
            return SecretKeySpec(entry.secretKey.encoded, "AES")
        }
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store.setEntry(
            alias,
            KeyStore.SecretKeyEntry(key),
            KeyStore.PasswordProtection(storePassword),
        )
        storeFile.outputStream().use { store.store(it, storePassword) }
        return key
    }
}
