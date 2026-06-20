package eu.ejdr.presentation.features.charactersheet

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.charactersheet.page.MyCharacterSheetsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

/**
 * Entries de navigation des fiches de personnage (Android).
 *
 * Pour l'instant seul l'écran liste ([Route.CharacterSheets], onglet de 1er niveau) est rendu ;
 * le détail ([Route.CharacterSheetDetail]) sera ajouté ensuite (en attendant, le fallback de
 * l'AppNavDisplay affiche un placeholder).
 */
fun EntryProviderScope<Any>.characterSheetEntries(actions: NavActions) {
    entry<Route.CharacterSheets> {
        MyCharacterSheetsPage(
            onOpenSheet = { id, name -> actions.backStack.add(Route.CharacterSheetDetail(id, name)) },
        )
    }
}
