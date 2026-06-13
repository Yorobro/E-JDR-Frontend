package eu.ejdr.presentation.features.charactersheet

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.charactersheet.page.MyCharacterSheetsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature fiches (écran « Mes fiches »). */
fun EntryProviderScope<Any>.characterSheetEntries(actions: NavActions) {
    entry<Route.CharacterSheets> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Mes fiches",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            MyCharacterSheetsPage()
        }
    }
}
