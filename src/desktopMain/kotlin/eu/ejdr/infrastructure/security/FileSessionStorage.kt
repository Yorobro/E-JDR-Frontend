package eu.ejdr.infrastructure.security

import java.io.File

class FileSessionStorage(
    dataDir: File,
    private val cipher: CookieCipher,
) : SessionStorage {

    private val storeFile = File(dataDir, "secure-cookies.enc")

    override fun load(): String? {
        if (!storeFile.exists()) return null
        return runCatching { cipher.decrypt(storeFile.readText()) }.getOrNull()
    }

    override fun save(value: String) {
        storeFile.writeText(cipher.encrypt(value))
    }

    override fun clear() {
        if (storeFile.exists()) storeFile.delete()
    }

    override fun exists(): Boolean = storeFile.exists()
}
