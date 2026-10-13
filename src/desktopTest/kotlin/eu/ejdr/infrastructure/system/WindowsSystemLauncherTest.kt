package eu.ejdr.infrastructure.system

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Teste la construction (pure) des scripts d'installation passive + relance.
 *
 * L'exécution réelle (`ProcessBuilder.start()` + `exitProcess`) n'est pas testable en unitaire
 * (elle tue la JVM) : seuls les scripts produits sont vérifiés ici.
 */
class WindowsSystemLauncherTest {

    private val installer = "C:\\tmp\\E-JDR-update.exe"
    private val appExe = "C:\\Program Files\\E-JDR\\E-JDR.exe"

    private fun decode(encoded: String) = String(Base64.getDecoder().decode(encoded), Charsets.UTF_16LE)

    @Test
    fun `installe en passif avec barre de progression, sans restart, et journalise`() {
        val script = buildUpdateScript(installer, appExe)

        assertContains(script, "Start-Process -FilePath 'C:\\tmp\\E-JDR-update.exe'")
        assertContains(script, "-ArgumentList '/passive /norestart /l*v \"C:\\tmp\\E-JDR-update.exe.log\"' -Wait")
    }

    @Test
    fun `n'utilise aucune option Inno Setup, inconnue de msiexec`() {
        val script = buildUpdateScript(installer, appExe)

        listOf("/SILENT", "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART").forEach { innoFlag ->
            assertFalse(script.contains(innoFlag), "$innoFlag bloque msiexec sans fenêtre visible")
        }
    }

    @Test
    fun `relance l'application meme si l'installation echoue ou est refusee`() {
        val script = buildUpdateScript(installer, appExe)

        assertContains(script, "finally { Start-Sleep -Seconds 2; Start-Process -FilePath '$appExe' }")
    }

    @Test
    fun `n'ajoute pas de relance quand le chemin de l'app est inconnu (dev)`() {
        val script = buildUpdateScript(installer, null)

        assertFalse(script.contains("finally"), "aucune relance sans appExe")
        assertContains(script, "/passive")
    }

    @Test
    fun `echappe les apostrophes des chemins`() {
        val script = buildUpdateScript("C:\\Users\\O'Neil\\Temp\\E-JDR-update.exe", null)

        assertContains(script, "'C:\\Users\\O''Neil\\Temp\\E-JDR-update.exe'")
    }

    @Test
    fun `cree l'orchestrateur hors du job via WMI, cache, depuis le dossier de l'installeur`() {
        val script = buildDetachedLaunchScript(installer, "C:\\tmp", appExe)

        assertContains(script, "Invoke-CimMethod -ClassName Win32_Process -MethodName Create")
        assertContains(script, "CurrentDirectory = 'C:\\tmp'")
        assertContains(script, "ShowWindow = [uint16]0")
        assertContains(script, "exit \$r.ReturnValue")
    }

    @Test
    fun `l'orchestrateur execute exactement le script de mise a jour`() {
        val script = buildDetachedLaunchScript(installer, "C:\\tmp", appExe)
        val encoded = Regex("-EncodedCommand (\\S+)'").find(script)!!.groupValues[1]

        assertEquals(buildUpdateScript(installer, appExe), decode(encoded))
    }

    @Test
    fun `encode en base64 UTF-16LE pour -EncodedCommand`() {
        assertEquals("ZQB4AGkAdAA=", encodePowerShell("exit"))
    }
}
