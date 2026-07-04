package eu.ejdr.infrastructure.security

import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Gère un KeyStore JCEKS local contenant une clé AES.
 * La clé sert à chiffrer le refresh_token persisté.
 *
 * Le mot de passe du coffre est **aléatoire** (SecureRandom) et n'est jamais stocké en clair :
 * seul son chiffré par un [SecretProtector] (DPAPI sous Windows, lié à l'utilisateur courant)
 * est persisté dans `store.pwd`. Un autre compte du poste, même avec un accès en lecture aux
 * fichiers de l'application, ne peut donc pas reconstituer ce mot de passe — ce qui corrige la
 * faiblesse de l'ancienne dérivation à partir d'attributs publics (user.name/home/os.name).
 *
 * Le `storePassword` reste injectable (tests) ; à défaut il est résolu via [resolveStorePassword].
 */
class KeyStoreProvider(
    dataDir: File,
    private val storePassword: CharArray = resolveStorePassword(dataDir, PlaintextSecretProtector()),
) {
    constructor(
        dataDir: File,
        protector: SecretProtector,
    ) : this(dataDir, resolveStorePassword(dataDir, protector))

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
        /** Fichier portant le mot de passe du coffre, chiffré par le [SecretProtector]. */
        private const val PASSWORD_FILE = "store.pwd"

        /** Longueur du mot de passe aléatoire (octets) avant encodage Base64. */
        private const val PASSWORD_BYTES = 32

        /**
         * Résout le mot de passe du coffre : déchiffre `store.pwd` via [protector] s'il existe,
         * sinon génère un mot de passe aléatoire, le persiste **chiffré** par [protector], et le
         * renvoie.
         *
         * Si le fichier existe mais est indéchiffrable (protégé par un AUTRE utilisateur, ou
         * corrompu : [protector] lève), on régénère un mot de passe neuf — le coffre redeviendra
         * alors illisible et sera réinitialisé (simple reconnexion), plutôt que de planter.
         */
        private fun resolveStorePassword(dataDir: File, protector: SecretProtector): CharArray {
            val passwordFile = File(dataDir, PASSWORD_FILE)
            if (passwordFile.exists()) {
                runCatching {
                    val revealed = protector.reveal(passwordFile.readBytes())
                    return revealed.toString(Charsets.UTF_8).toCharArray()
                }
                // Indéchiffrable (autre utilisateur / corruption) : on régénère ci-dessous.
            }
            return generateAndPersist(dataDir, passwordFile, protector)
        }

        private fun generateAndPersist(
            dataDir: File,
            passwordFile: File,
            protector: SecretProtector,
        ): CharArray {
            dataDir.mkdirs()
            val raw = ByteArray(PASSWORD_BYTES).also { SecureRandom().nextBytes(it) }
            val password = Base64.getEncoder().encodeToString(raw)
            val bytes = password.toByteArray(Charsets.UTF_8)
            passwordFile.writeBytes(protector.protect(bytes))
            return password.toCharArray()
        }
    }
}
