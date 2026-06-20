package eu.ejdr.presentation.features.settings

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

/** Entry des paramètres (Android) : onglet de premier niveau, pas de top-bar. */
fun EntryProviderScope<Any>.settingsEntries(actions: NavActions) {
    entry<Route.Settings> {
        SettingsPage(onThemeChange = actions.onThemeChange)
    }
}
