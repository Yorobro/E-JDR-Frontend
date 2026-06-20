package eu.ejdr.infrastructure.security

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CookieCipherTest {

    private val tmpDir = Files.createTempDirectory("ejdr-test").toFile()
    private val cipher = CookieCipher(KeyStoreProvider(tmpDir))

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `encrypt then decrypt returns original`() {
        val plaintext = "refresh_token=abc.def.ghi"
        val encrypted = cipher.encrypt(plaintext)
        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, cipher.decrypt(encrypted))
    }
}
