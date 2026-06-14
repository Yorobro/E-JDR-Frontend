package eu.ejdr.presentation.features.charactersheet

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.charactersheet.page.CharacterSheetDetailPage
import eu.ejdr.presentation.features.charactersheet.page.MyCharacterSheetsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature fiches (liste « Mes fiches » + détail d'une fiche). */
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
            MyCharacterSheetsPage(
                onOpenSheet = { id, name -> actions.backStack.add(Route.CharacterSheetDetail(id, name)) },
            )
        }
    }
    entry<Route.CharacterSheetDetail> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.name,
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            CharacterSheetDetailPage(id = key.id, name = key.name)
        }
    }
}
