package eu.ejdr.presentation.features.settings

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.navigation.MainTopBar
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold

fun EntryProviderScope<Any>.settingsEntries(actions: NavActions) {
    entry<Route.Settings> {
        AppScaffold(
            topBar = { MainTopBar(title = "Paramètres", currentRoute = Route.Settings, actions = actions) },
        ) {
            SettingsPage(onThemeChange = actions.onThemeChange)
        }
    }
}
