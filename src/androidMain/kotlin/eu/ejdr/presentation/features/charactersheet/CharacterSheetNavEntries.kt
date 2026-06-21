package eu.ejdr.presentation.features.charactersheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.charactersheet.page.CharacterSheetDetailPage
import eu.ejdr.presentation.features.charactersheet.page.MyCharacterSheetsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/**
 * Entries de navigation des fiches de personnage (Android).
 *
 * [Route.CharacterSheets] : onglet de 1er niveau (liste), pas de top-bar.
 * [Route.CharacterSheetDetail] : sous-écran (4 onglets) avec [AppTopBar] et bouton retour.
 */
fun EntryProviderScope<Any>.characterSheetEntries(actions: NavActions) {
    entry<Route.CharacterSheets> {
        MyCharacterSheetsPage(
            onOpenSheet = { id, name -> actions.backStack.add(Route.CharacterSheetDetail(id, name)) },
        )
    }

    entry<Route.CharacterSheetDetail> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.name, onBack = { actions.backStack.removeLastOrNull() })
            CharacterSheetDetailPage(id = key.id, name = key.name, modifier = Modifier.weight(1f))
        }
    }
}
