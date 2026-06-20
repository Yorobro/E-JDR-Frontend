package eu.ejdr.infrastructure.security

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureCookiesStorageTest {

    private val tmpDir = Files.createTempDirectory("ejdr-cookies").toFile()
    private val cipher = CookieCipher(KeyStoreProvider(tmpDir))
    private val backendUrl = "https://ejdr-backend.vyxs.fr"

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    private fun newSessionStorage() = FileSessionStorage(tmpDir, cipher)

    private fun newStorage() = SecureCookiesStorage(newSessionStorage(), backendUrl, AcceptAllCookiesStorage())

    @Test
    fun `persists refresh_token and reports hasPersistedSession`() = runTest {
        val storage = newStorage()
        storage.addCookie(Url(backendUrl), Cookie("refresh_token", "tok123"))

        assertTrue(storage.hasPersistedSession())
    }

    @Test
    fun `restores persisted refresh_token for the backend host on a fresh instance`() = runTest {
        newStorage().addCookie(Url(backendUrl), Cookie("refresh_token", "tok123"))

        val reloaded = newStorage()
        val cookies = reloaded.get(Url("$backendUrl/auth/refresh"))

        assertEquals("tok123", cookies.firstOrNull { it.name == "refresh_token" }?.value)
    }

    @Test
    fun `does not leak the persisted refresh_token to a foreign host`() = runTest {
        newStorage().addCookie(Url(backendUrl), Cookie("refresh_token", "tok123"))

        val reloaded = newStorage()
        val githubCookies = reloaded.get(Url("https://api.github.com/repos/owner/repo/releases/latest"))

        assertNull(githubCookies.firstOrNull { it.name == "refresh_token" })
    }

    @Test
    fun `still serves the refresh_token to the backend after a foreign host request was made first`() = runTest {
        newStorage().addCookie(Url(backendUrl), Cookie("refresh_token", "tok123"))

        val reloaded = newStorage()
        // Simule la course au démarrage : l'appel GitHub (CheckUpdate) touche get() en premier.
        reloaded.get(Url("https://api.github.com/repos/owner/repo/releases/latest"))
        // La restauration de session doit malgré tout emporter le refresh_token.
        val backendCookies = reloaded.get(Url("$backendUrl/auth/refresh"))

        assertEquals("tok123", backendCookies.firstOrNull { it.name == "refresh_token" }?.value)
    }

    @Test
    fun `clear removes persisted file`() = runTest {
        val storage = newStorage()
        storage.addCookie(Url(backendUrl), Cookie("refresh_token", "tok123"))
        storage.clearPersisted()

        assertFalse(storage.hasPersistedSession())
    }
}
