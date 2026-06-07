package eu.ejdr.infrastructure.security

import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Gère un KeyStore JCEKS local contenant une clé AES.
 * La clé sert à chiffrer le refresh_token persisté.
 *
 * Le mot de passe du coffre n'est plus un littéral en clair dans le binaire : il est
 * **dérivé d'attributs locaux** (utilisateur + machine) via SHA-256. Sur un poste donné,
 * la dérivation est stable (le coffre se rouvre), mais un binaire copié sur une autre
 * session/machine ne peut pas déchiffrer le coffre d'origine. Ce n'est pas un secret
 * matériel (un attaquant avec accès complet au poste pourrait le reconstituer), mais cela
 * supprime le mot de passe codé en dur et lie le coffre à son environnement.
 */
class KeyStoreProvider(
    dataDir: File,
    private val storePassword: CharArray = deriveStorePassword(),
) {
    private val storeFile = File(dataDir, "ejdr.jceks")
    private val alias = "cookie-key"

    fun getOrCreateKey(): SecretKey {
        // Un coffre existant mais **illisible** (mot de passe changé, fichier corrompu) ne doit
        // jamais faire planter l'application au démarrage : on repart alors d'un coffre neuf.
        // Conséquence : le refresh_token chiffré avec l'ancienne clé devient inexploitable, ce
        // qui force une simple reconnexion — bien préférable à un crash.
        val store = loadStoreOrReset()

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

    /**
     * Charge le KeyStore existant, ou en initialise un neuf s'il est absent ou illisible.
     *
     * @return Un KeyStore JCEKS chargé, prêt à être lu/écrit avec [storePassword].
     */
    private fun loadStoreOrReset(): KeyStore {
        val store = KeyStore.getInstance("JCEKS")
        if (!storeFile.exists()) {
            store.load(null, storePassword)
            return store
        }
        return try {
            storeFile.inputStream().use { store.load(it, storePassword) }
            store
        } catch (_: Exception) {
            // Coffre illisible : on le supprime et on repart d'un coffre vide.
            storeFile.delete()
            KeyStore.getInstance("JCEKS").apply { load(null, storePassword) }
        }
    }

    companion object {
        /** Préfixe de domaine pour éviter toute collision avec d'autres usages du même secret dérivé. */
        private const val DERIVATION_SALT = "ejdr-keystore-v1"

        /**
         * Dérive le mot de passe du coffre à partir d'attributs de l'environnement local
         * (nom d'utilisateur, répertoire personnel, OS), condensés en SHA-256.
         *
         * Stable sur un même poste/session ; différent ailleurs. Remplace le mot de passe
         * codé en dur.
         */
        private fun deriveStorePassword(): CharArray {
            val material =
                buildString {
                    append(DERIVATION_SALT)
                    append('|')
                    append(System.getProperty("user.name").orEmpty())
                    append('|')
                    append(System.getProperty("user.home").orEmpty())
                    append('|')
                    append(System.getProperty("os.name").orEmpty())
                }
            val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(digest).toCharArray()
        }
    }
}
