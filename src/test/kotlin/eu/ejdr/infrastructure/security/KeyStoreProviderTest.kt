package eu.ejdr.infrastructure.security

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyStoreProviderTest {

    private val tmpDir = Files.createTempDirectory("ejdr-keystore").toFile()

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `returns a stable key across instances with the same password`() {
        val pwd = "same-password".toCharArray()
        val first = KeyStoreProvider(tmpDir, pwd).getOrCreateKey()
        val second = KeyStoreProvider(tmpDir, pwd).getOrCreateKey()

        assertContentEquals(first.encoded, second.encoded)
    }

    @Test
    fun `resets the store instead of throwing when the password changed`() {
        // Coffre créé avec un premier mot de passe (simule l'ancienne version en dur).
        KeyStoreProvider(tmpDir, "old-password".toCharArray()).getOrCreateKey()

        // Ouverture avec un mot de passe différent (simule la dérivation P05) : ne doit PAS
        // lever, mais repartir d'un coffre neuf.
        val key = KeyStoreProvider(tmpDir, "new-password".toCharArray()).getOrCreateKey()

        assertNotNull(key)
        assertTrue(key.encoded.isNotEmpty())
    }
}
