package eu.ejdr.presentation.features.settings

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entry de navigation de l'écran des paramètres. */
fun EntryProviderScope<Any>.settingsEntries(actions: NavActions) {
    entry<Route.Settings> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Paramètres",
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            SettingsPage(onThemeChange = actions.onThemeChange)
        }
    }
}
