package eu.ejdr.infrastructure.security

import com.sun.jna.platform.win32.Crypt32Util

/**
 * [SecretProtector] adossé à **DPAPI** (Windows Data Protection API), portée utilisateur.
 *
 * `CryptProtectData`/`CryptUnprotectData` chiffrent avec une clé dérivée des identifiants de
 * connexion de l'utilisateur Windows courant : le blob produit n'est déchiffrable que par ce
 * même utilisateur sur ce poste. Un autre compte lisant les fichiers de l'application ne peut
 * donc PAS récupérer le secret protégé — ce qui corrige la faiblesse de la dérivation à partir
 * d'attributs publics (user.name/home/os.name) qui était reconstituable.
 */
class DpapiSecretProtector : SecretProtector {
    override fun protect(data: ByteArray): ByteArray = Crypt32Util.cryptProtectData(data)

    override fun reveal(data: ByteArray): ByteArray = Crypt32Util.cryptUnprotectData(data)
}
