package eu.ejdr.infrastructure.system

import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import java.io.File
import kotlin.system.exitProcess

/**
 * Implémentation Windows de [SystemLauncherService].
 *
 * Lance l'installeur via `cmd /c start` (détaché du processus courant) puis quitte
 * l'application avec un code de succès afin de libérer les fichiers et laisser
 * l'installeur s'exécuter. Ces effets de bord OS sont volontairement confinés à
 * l'infrastructure : la couche application ne dépend que du service [SystemLauncherService].
 */
class WindowsSystemLauncher : SystemLauncherService {
    override fun launchInstallerAndExit(installer: File): Nothing {
        ProcessBuilder("cmd", "/c", "start", "", installer.absolutePath).start()
        exitProcess(0)
    }
}
