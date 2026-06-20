package eu.ejdr.infrastructure.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/** Chiffre/déchiffre des chaînes via AES-GCM avec la clé du KeyStore. */
class CookieCipher(keyStoreProvider: KeyStoreProvider) {

    private val key = keyStoreProvider.getOrCreateKey()
    private val ivLength = 12
    private val tagLength = 128

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(ivLength).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(tagLength, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encoded: String): String {
        val combined = Base64.getDecoder().decode(encoded)
        val iv = combined.copyOfRange(0, ivLength)
        val encrypted = combined.copyOfRange(ivLength, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagLength, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
