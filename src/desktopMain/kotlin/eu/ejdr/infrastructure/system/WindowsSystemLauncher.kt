package eu.ejdr.infrastructure.system

import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import java.io.File
import kotlin.system.exitProcess

/**
 * Implémentation Windows de [SystemLauncherService].
 *
 * Lance l'installeur de mise à jour en mode **silencieux** (`/SILENT` : barre de progression
 * Inno Setup, sans assistant), attend sa fin, puis **relance automatiquement** l'application
 * fraîchement réinstallée. Le tout est exécuté dans un `cmd` détaché afin de pouvoir quitter
 * immédiatement la JVM (`exitProcess(0)`) et libérer les verrous de fichiers que l'installeur
 * doit écraser.
 *
 * L'assistant interactif n'apparaît donc plus lors des mises à jour ; la première installation
 * (lancée à la main par l'utilisateur, hors de ce launcher) garde son installeur complet.
 *
 * Ces effets de bord OS sont volontairement confinés à l'infrastructure : la couche application
 * ne dépend que du service [SystemLauncherService].
 */
class WindowsSystemLauncher : SystemLauncherService {
    override fun launchInstallerAndExit(installer: File): Nothing {
        val command = buildInstallCommand(installer.absolutePath, currentExePath())
        ProcessBuilder("cmd", "/c", command).start()
        exitProcess(0)
    }

    /**
     * Chemin de l'exécutable de l'application courante, ou `null` si on ne tourne pas depuis un
     * `.exe` packagé (ex. lancement via la JVM en développement : le process est `java.exe`, qu'il
     * ne faut pas relancer). Grâce à `upgradeUuid` le dossier d'installation est conservé d'une
     * version à l'autre : ce chemin reste donc valide après la mise à jour.
     */
    private fun currentExePath(): String? =
        ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { it.endsWith(".exe", ignoreCase = true) }
            ?.let { File(it).absolutePath }
}

/**
 * Construit la ligne de commande `cmd` qui installe la mise à jour en silence puis relance l'app.
 *
 * Fonction pure (testable sans effet de bord) : `start "" /wait` exécute l'installeur Inno Setup
 * avec `/SILENT /SUPPRESSMSGBOXES /NORESTART` et attend sa fin, un court `timeout` laisse l'OS
 * libérer les fichiers, puis `start "" "<appExe>"` relance l'application. Si [appExe] est `null`
 * (dev), le segment de relance est omis : on installe sans relancer.
 *
 * @param installerPath Chemin absolu de l'installeur téléchargé.
 * @param appExe Chemin absolu de l'exécutable installé à relancer, ou `null` pour ne pas relancer.
 */
internal fun buildInstallCommand(installerPath: String, appExe: String?): String {
    val install = "start \"\" /wait \"$installerPath\" /SILENT /SUPPRESSMSGBOXES /NORESTART"
    return if (appExe != null) {
        "$install & timeout /t 2 /nobreak >nul & start \"\" \"$appExe\""
    } else {
        install
    }
}
