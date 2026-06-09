package eu.ejdr.infrastructure.security

import eu.ejdr.application.auth.abstraction.service.SessionPersistence
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import java.io.File

/**
 * Décore un CookiesStorage en mémoire. Persiste UNIQUEMENT le refresh_token,
 * chiffré sur disque, pour permettre l'auto-login après redémarrage.
 * L'access_token reste en mémoire seulement.
 *
 * Le refresh_token chargé depuis le disque est ré-injecté paresseusement au premier
 * appel à [get] : cela évite un `runBlocking` dans le constructeur et utilise l'URL
 * réelle de la requête plutôt qu'une URL codée en dur.
 */
class SecureCookiesStorage(
    dataDir: File,
    private val cipher: CookieCipher,
    private val delegate: CookiesStorage,
) : CookiesStorage, SessionPersistence {

    private val refreshTokenName = "refresh_token"
    private val storeFile = File(dataDir, "secure-cookies.enc")
    private var pendingRefreshToken: String? = loadPersistedValue()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        delegate.addCookie(requestUrl, cookie)
        if (cookie.name == refreshTokenName) {
            persist(cookie.value)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        pendingRefreshToken?.let { value ->
            pendingRefreshToken = null
            delegate.addCookie(requestUrl, Cookie(name = refreshTokenName, value = value))
        }
        return delegate.get(requestUrl)
    }

    override fun close() = delegate.close()

    override fun hasPersistedSession(): Boolean = storeFile.exists()

    override fun clearPersisted() {
        if (storeFile.exists()) storeFile.delete()
    }

    private fun persist(value: String) {
        storeFile.writeText(cipher.encrypt(value))
    }

    private fun loadPersistedValue(): String? {
        if (!storeFile.exists()) return null
        return runCatching { cipher.decrypt(storeFile.readText()) }.getOrNull()
    }
}
