package eu.ejdr.infrastructure.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import java.io.File

/**
 * Implémentation Android de [SystemLauncherService].
 *
 * Sur Android, les mises à jour passent par le **Play Store** : il n'y a pas d'installeur local à
 * lancer. L'UI Android n'appelle donc jamais le flux « télécharger + installer » ; elle ouvre
 * directement la fiche Play Store via [openStore]. [launchInstallerAndExit] reste implémenté pour
 * satisfaire le contrat commun mais redirige vers le store (et n'est, en pratique, jamais atteint).
 */
class AndroidUpdateLauncher(private val context: Context) : SystemLauncherService {

    /** Ouvre la fiche Play Store de l'application (repli navigateur si le Play Store est absent). */
    fun openStore() {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${context.packageName}"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        runCatching { context.startActivity(market) }.onFailure {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(web)
        }
    }

    override fun launchInstallerAndExit(installer: File): Nothing {
        openStore()
        error("Android n'installe pas d'installeur local : mise à jour déléguée au Play Store.")
    }
}
