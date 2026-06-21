package eu.ejdr.infrastructure.file

import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Implémentation desktop de [FileSaver] : `java.awt.FileDialog` natif « Enregistrer sous ».
 *
 * Le dialogue AWT est bloquant et doit s'exécuter sur le thread d'événements AWT (EDT) :
 * on bascule sur [Dispatchers.Swing]. L'écriture disque suit immédiatement.
 */
class DesktopFileSaver : FileSaver {
    override suspend fun save(suggestedName: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.Swing) {
            val dialog = FileDialog(null as Frame?, "Enregistrer la fiche", FileDialog.SAVE)
            dialog.file = suggestedName
            dialog.isVisible = true
            val dir = dialog.directory
            val name = dialog.file
            if (dir == null || name == null) {
                false
            } else {
                File(dir, name).outputStream().use { it.write(bytes) }
                true
            }
        }
}
