package eu.ejdr.infrastructure.file

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implémentation Android de [FileSaver].
 *
 * Écrit le binaire dans le cache de l'app puis ouvre une *share sheet* (ACTION_SEND) via un
 * [FileProvider] : l'utilisateur choisit où enregistrer/partager le PDF. Équivalent mobile du
 * dialogue « Enregistrer sous » desktop.
 */
class AndroidFileSaver(private val context: Context) : FileSaver {

    override suspend fun save(suggestedName: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.cacheDir, suggestedName)
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, suggestedName).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
                true
            }.getOrDefault(false)
        }
}
