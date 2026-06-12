package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import eu.ejdr.domain.features.settings.entities.ThemeVariant

/**
 * Actions de navigation transverses fournies par l'app aux entries de chaque feature.
 *
 * Regroupe les actions communes (déconnexion, changement de thème, reset de pile, accès au
 * back-stack) en un seul objet, au lieu de propager N callbacks séparés à chaque feature.
 * Ajouter une action transverse = un champ ici, pas un n-ième paramètre.
 *
 * @property backStack Pile possédée par l'app (empiler/dépiler des [Route]).
 * @property onLogout Déconnexion (use case + retour Login), déléguée à l'app.
 * @property onThemeChange Propage le thème choisi à l'app pour recomposer le design system.
 * @property resetTo Remplace toute la pile par une destination unique (post-login/logout).
 */
class NavActions(
    val backStack: NavBackStack<NavKey>,
    val onLogout: () -> Unit,
    val onThemeChange: (ThemeVariant) -> Unit,
    val resetTo: (Route) -> Unit,
)
