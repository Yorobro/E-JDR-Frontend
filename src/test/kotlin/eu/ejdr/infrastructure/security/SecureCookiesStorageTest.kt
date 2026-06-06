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
import kotlin.test.assertTrue

class SecureCookiesStorageTest {

    private val tmpDir = Files.createTempDirectory("ejdr-cookies").toFile()
    private val cipher = CookieCipher(KeyStoreProvider(tmpDir))

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    private fun newStorage() = SecureCookiesStorage(tmpDir, cipher, AcceptAllCookiesStorage())

    @Test
    fun `persists refresh_token and reports hasPersistedSession`() = runTest {
        val storage = newStorage()
        storage.addCookie(Url("http://localhost"), Cookie("refresh_token", "tok123"))

        assertTrue(storage.hasPersistedSession())
    }

    @Test
    fun `restores persisted refresh_token in a fresh storage instance`() = runTest {
        newStorage().addCookie(Url("http://localhost"), Cookie("refresh_token", "tok123"))

        val reloaded = newStorage()
        val cookies = reloaded.get(Url("http://localhost"))

        assertEquals("tok123", cookies.firstOrNull { it.name == "refresh_token" }?.value)
    }

    @Test
    fun `clear removes persisted file`() = runTest {
        val storage = newStorage()
        storage.addCookie(Url("http://localhost"), Cookie("refresh_token", "tok123"))
        storage.clearPersisted()

        assertFalse(storage.hasPersistedSession())
    }
}
