package eu.ejdr.infrastructure.security

/**
 * [SecretProtector] de **repli pour les OS sans DPAPI** (Linux/macOS) : ne chiffre pas, rend
 * la donnée telle quelle.
 *
 * L'application cible Windows (installeur .exe/.msi, DPAPI disponible) ; ce repli évite un
 * crash sur d'autres OS au prix d'une absence de protection OS du mot de passe du KeyStore.
 * À remplacer par un équivalent (Keychain macOS, libsecret Linux) si ces plateformes
 * deviennent supportées.
 */
class PlaintextSecretProtector : SecretProtector {
    override fun protect(data: ByteArray): ByteArray = data

    override fun reveal(data: ByteArray): ByteArray = data
}
