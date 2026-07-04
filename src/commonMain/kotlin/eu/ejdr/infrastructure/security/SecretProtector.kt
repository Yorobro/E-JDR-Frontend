package eu.ejdr.infrastructure.security

/**
 * Chiffre/déchiffre un secret local en le liant à un contexte non lisible par un autre
 * utilisateur du poste (idéalement un mécanisme OS comme DPAPI sous Windows).
 *
 * Sert à protéger AU REPOS le mot de passe du KeyStore : ce mot de passe est aléatoire
 * (SecureRandom) et seul son chiffré DPAPI est persisté, si bien qu'un autre compte ne peut
 * pas le déchiffrer même avec un accès en lecture aux fichiers de l'application.
 */
interface SecretProtector {
    /** Chiffre [data] (portée utilisateur courant). */
    fun protect(data: ByteArray): ByteArray

    /** Déchiffre un blob produit par [protect] ; lève si le contexte (utilisateur) diffère. */
    fun reveal(data: ByteArray): ByteArray
}
