package eu.ejdr.infrastructure.security

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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

    /** Protecteur factice : simule DPAPI en préfixant les octets (chiffrement réversible trivial). */
    private class ReversibleProtector : SecretProtector {
        override fun protect(data: ByteArray) = ByteArray(1) { 0x42 } + data
        override fun reveal(data: ByteArray) = data.copyOfRange(1, data.size)
    }

    @Test
    fun `persists a protected random password and reuses it across instances`() {
        val protector = ReversibleProtector()

        val first = KeyStoreProvider(tmpDir, protector).getOrCreateKey()
        // Le mot de passe aléatoire est persisté CHIFFRÉ (jamais en clair) dans store.pwd.
        val pwdFile = tmpDir.resolve("store.pwd")
        assertTrue(pwdFile.exists())
        assertEquals(0x42.toByte(), pwdFile.readBytes().first()) // marqueur du protecteur

        // Une seconde instance déchiffre le même mot de passe et rouvre le MÊME coffre → même clé.
        val second = KeyStoreProvider(tmpDir, protector).getOrCreateKey()
        assertContentEquals(first.encoded, second.encoded)
    }
}
