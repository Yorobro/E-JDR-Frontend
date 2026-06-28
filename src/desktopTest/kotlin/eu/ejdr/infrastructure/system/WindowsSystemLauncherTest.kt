package eu.ejdr.infrastructure.system

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Teste la construction (pure) de la commande d'installation silencieuse + relance.
 *
 * L'exécution réelle (`ProcessBuilder.start()` + `exitProcess`) n'est pas testable en unitaire
 * (elle tue la JVM) : seule la ligne de commande produite est vérifiée ici.
 */
class WindowsSystemLauncherTest {

    @Test
    fun `installe en silence avec barre de progression et sans message box ni restart`() {
        val cmd = buildInstallCommand("C:\\tmp\\E-JDR-update.exe", "C:\\Apps\\E-JDR\\E-JDR.exe")

        assertContains(cmd, "/SILENT")
        assertContains(cmd, "/SUPPRESSMSGBOXES")
        assertContains(cmd, "/NORESTART")
        assertContains(cmd, "start \"\" /wait \"C:\\tmp\\E-JDR-update.exe\"")
    }

    @Test
    fun `relance l'application installee quand son chemin est connu`() {
        val cmd = buildInstallCommand("C:\\tmp\\E-JDR-update.exe", "C:\\Apps\\E-JDR\\E-JDR.exe")

        assertContains(cmd, "start \"\" \"C:\\Apps\\E-JDR\\E-JDR.exe\"")
        assertTrue(cmd.contains("&"), "les deux étapes doivent être chaînées")
    }

    @Test
    fun `n'ajoute pas de relance quand le chemin de l'app est inconnu (dev)`() {
        val cmd = buildInstallCommand("C:\\tmp\\E-JDR-update.exe", null)

        assertContains(cmd, "/SILENT")
        assertFalse(cmd.contains("& start"), "aucune relance ne doit être chaînée sans appExe")
    }
}
