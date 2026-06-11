package eu.ejdr.application.features.update.abstraction

import java.io.File

/**
 * Port d'accès aux actions **système** liées à l'installation d'une mise à jour.
 *
 * Isole les effets de bord dépendants de l'OS (lancement d'un processus externe,
 * terminaison de l'application) hors de la couche application. Un use case reste
 * ainsi **pur et testable** : il décrit *quoi* faire (installer puis quitter) sans
 * connaître *comment* (cmd Windows, exit JVM…), détail confié à l'implémentation
 * d'infrastructure.
 */
interface SystemLauncher {
    /**
     * Lance l'installeur téléchargé puis **termine** l'application courante pour
     * lui céder la place.
     *
     * Le type de retour [Nothing] encode dans la signature que l'appel ne rend
     * jamais la main (le processus se termine).
     *
     * @param installer Fichier exécutable de mise à jour à lancer.
     */
    fun launchInstallerAndExit(installer: File): Nothing
}
