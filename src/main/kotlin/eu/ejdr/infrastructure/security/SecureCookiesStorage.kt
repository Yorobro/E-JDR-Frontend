package eu.ejdr.infrastructure.security

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Décore un CookiesStorage en mémoire. Persiste UNIQUEMENT le refresh_token,
 * chiffré sur disque, pour permettre l'auto-login après redémarrage.
 * L'access_token reste en mémoire seulement.
 */
class SecureCookiesStorage(
    dataDir: File,
    private val cipher: CookieCipher,
    private val delegate: CookiesStorage,
) : CookiesStorage {

    private val refreshTokenName = "refresh_token"
    private val storeFile = File(dataDir, "secure-cookies.enc")

    init {
        loadPersisted()
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        delegate.addCookie(requestUrl, cookie)
        if (cookie.name == refreshTokenName) {
            persist(cookie.value)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = delegate.get(requestUrl)

    override fun close() = delegate.close()

    fun hasPersistedSession(): Boolean = storeFile.exists()

    fun clearPersisted() {
        if (storeFile.exists()) storeFile.delete()
    }

    private fun persist(value: String) {
        storeFile.writeText(cipher.encrypt(value))
    }

    private fun loadPersisted() {
        if (!storeFile.exists()) return
        val value = runCatching { cipher.decrypt(storeFile.readText()) }.getOrNull() ?: return
        // ré-injecte le refresh_token en mémoire pour le prochain /auth/refresh
        runBlocking {
            delegate.addCookie(
                Url("http://localhost"),
                Cookie(name = refreshTokenName, value = value),
            )
        }
    }
}
