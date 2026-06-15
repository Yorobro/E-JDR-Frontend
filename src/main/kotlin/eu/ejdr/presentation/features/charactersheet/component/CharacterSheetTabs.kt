package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.theme.AppTheme

/* ----------------------------------------------------------------------------------------- *
 * Contenus des trois onglets de la fiche détail. Composants bêtes : empilent des [SheetCard]
 * réutilisant les sections existantes et partagent le même [CharacterSheetFormState] que la
 * page (édition persistante d'un onglet à l'autre).
 * ----------------------------------------------------------------------------------------- */

/** Colonne d'onglet : pleine largeur, cartes espacées de `lg`. Factorise la structure commune. */
@Composable
private fun TabColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) { content() }
}

/** Onglet Identité : Identité + Caractéristiques, empilés pleine largeur. */
@Composable
fun IdentiteTab(sheet: CharacterSheet, isEditing: Boolean, form: CharacterSheetFormState) {
    TabColumn {
        SheetCard("Identité") { IdentiteSection(sheet, isEditing, form) }
        SheetCard("Caractéristiques") { CaracteristiquesSection(sheet, isEditing, form) }
    }
}

/** Onglet Combat : [Combat · Bourse], [Armes · Armures], puis Sorts & Miracles pleine largeur. */
@Composable
fun CombatTab(sheet: CharacterSheet, isEditing: Boolean, form: CharacterSheetFormState) {
    TabColumn {
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Combat") { CombatSection(sheet, isEditing, form) } },
                { SheetCard("Bourse") { PurseSection(sheet, isEditing, form) } },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Armes") { LongTextBody(isEditing, form.armes, sheet.armes) { form.armes = it } } },
                { SheetCard("Armures") { LongTextBody(isEditing, form.armures, sheet.armures) { form.armures = it } } },
            ),
        )
        SheetCard("Sorts & Miracles") {
            LongTextBody(isEditing, form.sortsEtMiracles, sheet.sortsEtMiracles) { form.sortsEtMiracles = it }
        }
    }
}

/** Onglet Inventaire : [Équipement · Compétences], puis Notes pleine largeur. */
@Composable
fun InventaireTab(sheet: CharacterSheet, isEditing: Boolean, form: CharacterSheetFormState) {
    TabColumn {
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Équipement") { LongTextBody(isEditing, form.equipement, sheet.equipement) { form.equipement = it } } },
                { SheetCard("Compétences") { LongTextBody(isEditing, form.competences, sheet.competences) { form.competences = it } } },
            ),
        )
        SheetCard("Notes") { LongTextBody(isEditing, form.notes, sheet.notes) { form.notes = it } }
    }
}
