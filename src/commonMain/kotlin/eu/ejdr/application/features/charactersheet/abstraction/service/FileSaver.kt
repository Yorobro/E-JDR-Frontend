package eu.ejdr.application.features.charactersheet.abstraction.service

/**
 * Port d'enregistrement d'un fichier binaire via un dialogue natif « Enregistrer sous ».
 *
 * Abstraction plateforme : la couche application/présentation reste indépendante d'AWT/Swing
 * et testable. L'implémentation réelle (desktop) vit dans l'infrastructure.
 */
fun interface FileSaver {
    /**
     * Propose un dialogue « Enregistrer sous » puis écrit [bytes].
     *
     * @param suggestedName Nom de fichier proposé par défaut.
     * @param bytes Contenu binaire à écrire.
     * @return `true` si l'utilisateur a confirmé et le fichier a été écrit ; `false` si annulé.
     */
    suspend fun save(suggestedName: String, bytes: ByteArray): Boolean
}
