package eu.ejdr.infrastructure.system

import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Implémentation Windows de [SystemLauncherService].
 *
 * Lance l'installeur de mise à jour en mode **passif** (`/passive` : barre de progression
 * Windows Installer, sans assistant), attend sa fin, puis **relance automatiquement** l'application
 * fraîchement réinstallée, et quitte immédiatement la JVM (`exitProcess(0)`) pour libérer les
 * verrous de fichiers que l'installeur doit écraser.
 *
 * **Piège jpackage** : le lanceur `E-JDR.exe` exécute la JVM dans un *job object* Windows configuré
 * en `KILL_ON_JOB_CLOSE`. Tout processus lancé depuis la JVM (`ProcessBuilder`) entre dans ce job et
 * est **tué dès que l'application se ferme** — l'installeur disparaît alors en pleine demande
 * d'élévation UAC. L'orchestrateur est donc créé **hors du job** via WMI (`Win32_Process.Create`) :
 * c'est le service WMI qui crée le processus, pas la JVM. Repli si WMI échoue : `explorer.exe`, qui
 * délègue l'ouverture de l'installeur (assistant complet) au shell Windows, lui aussi hors du job.
 *
 * L'assistant interactif n'apparaît donc plus lors des mises à jour ; la première installation
 * (lancée à la main par l'utilisateur, hors de ce launcher) garde son installeur complet.
 *
 * Ces effets de bord OS sont volontairement confinés à l'infrastructure : la couche application
 * ne dépend que du service [SystemLauncherService].
 */
class WindowsSystemLauncher : SystemLauncherService {
    override fun launchInstallerAndExit(installer: File): Nothing {
        val script = buildDetachedLaunchScript(
            installerPath = installer.absolutePath,
            workingDir = installer.absoluteFile.parent,
            appExe = currentExePath(),
        )
        val launched = runCatching {
            val process = ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encodePowerShell(script),
            ).start()
            process.waitFor(WMI_LAUNCH_TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)
        if (!launched) {
            runCatching { ProcessBuilder("explorer.exe", installer.absolutePath).start() }
        }
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

    private companion object {
        const val WMI_LAUNCH_TIMEOUT_SECONDS = 20L
    }
}

/**
 * Script PowerShell qui installe la mise à jour puis relance l'app — exécuté **hors du job** de
 * l'application (cf. [buildDetachedLaunchScript]).
 *
 * L'`.exe` produit par jpackage n'est **pas** un installeur Inno Setup : c'est une enveloppe
 * autour d'un `.msi` qui transmet ses arguments tels quels à `msiexec /i main.msi`. Il faut donc
 * des options **msiexec** (`/passive /norestart`) ; une option Inno Setup (`/SILENT`…) fait
 * afficher l'aide de msiexec et bloque la mise à jour. `/l*v` écrit un journal détaillé
 * `<installeur>.log`, seul moyen de diagnostiquer une mise à jour ratée chez un utilisateur.
 *
 * `Start-Process` gère l'élévation UAC et affiche normalement les fenêtres qu'il lance. La relance
 * est dans un `finally` : si l'utilisateur refuse l'UAC, il retrouve son application (ancienne
 * version) au lieu de rester sans rien. Si [appExe] est `null` (dev), aucune relance.
 *
 * Fonction pure (testable sans effet de bord).
 */
internal fun buildUpdateScript(installerPath: String, appExe: String?): String {
    val msiArgs = "/passive /norestart /l*v \"$installerPath.log\""
    val install = "Start-Process -FilePath ${psQuote(installerPath)} -ArgumentList ${psQuote(msiArgs)} -Wait"
    if (appExe == null) return install
    return "try { $install } finally { Start-Sleep -Seconds 2; Start-Process -FilePath ${psQuote(appExe)} }"
}

/**
 * Script PowerShell, lancé depuis la JVM, qui crée via WMI (`Win32_Process.Create`) un PowerShell
 * caché exécutant [buildUpdateScript]. Le processus ainsi créé n'appartient pas au job de
 * l'application et survit donc à sa fermeture. Son dossier de travail est celui de l'installeur
 * (temp) : un processus resté dans le dossier d'installation serait fermé d'office par le Restart
 * Manager de msiexec. Le code de sortie est le `ReturnValue` de WMI (0 = processus créé).
 *
 * Fonction pure (testable sans effet de bord).
 */
internal fun buildDetachedLaunchScript(installerPath: String, workingDir: String, appExe: String?): String {
    val inner = encodePowerShell(buildUpdateScript(installerPath, appExe))
    val commandLine = "powershell.exe -NoProfile -NonInteractive -WindowStyle Hidden -EncodedCommand $inner"
    return """
        ${'$'}si = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ ShowWindow = [uint16]0 }
        ${'$'}r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = ${psQuote(commandLine)}; CurrentDirectory = ${psQuote(workingDir)}; ProcessStartupInformation = ${'$'}si }
        exit ${'$'}r.ReturnValue
    """.trimIndent()
}

/** Encode un script pour `powershell -EncodedCommand` (base64 de l'UTF-16LE) : aucun échappement. */
internal fun encodePowerShell(script: String): String =
    Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))

/** Chaîne PowerShell littérale (guillemets simples, `'` doublé). */
private fun psQuote(value: String): String = "'" + value.replace("'", "''") + "'"
