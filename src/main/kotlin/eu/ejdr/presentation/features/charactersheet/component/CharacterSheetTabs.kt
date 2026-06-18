package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.reference.entities.ReferenceType
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
fun IdentiteTab(
    sheet: CharacterSheet,
    isEditing: Boolean,
    form: CharacterSheetFormState,
    refs: SheetReferences,
) {
    TabColumn {
        SheetCard("Identité") { IdentiteSection(sheet, isEditing, form, refs) }
        SheetCard("Caractéristiques") { CaracteristiquesSection(sheet, isEditing, form) }
    }
}

/** Onglet Combat : [Combat · Bourse], [Armes · Armures] (N‑N), puis Sorts & Miracles. */
@Composable
fun CombatTab(
    sheet: CharacterSheet,
    isEditing: Boolean,
    form: CharacterSheetFormState,
    refs: SheetReferences,
) {
    TabColumn {
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Combat") { CombatSection(sheet, isEditing, form) } },
                { SheetCard("Bourse") { PurseSection(sheet, isEditing, form) } },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Armes") { LinkedReferenceSection(ReferenceType.ARME, isEditing, refs) } },
                { SheetCard("Armures") { LinkedReferenceSection(ReferenceType.ARMURE, isEditing, refs) } },
            ),
        )
        SheetCard("Sorts & Miracles") {
            LongTextBody(isEditing, form.sortsEtMiracles, sheet.sortsEtMiracles) { form.sortsEtMiracles = it }
        }
    }
}

/** Onglet Inventaire : [Équipements · Compétences] (N‑N), puis Notes pleine largeur. */
@Composable
fun InventaireTab(
    sheet: CharacterSheet,
    isEditing: Boolean,
    form: CharacterSheetFormState,
    refs: SheetReferences,
) {
    TabColumn {
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Équipements") { LinkedReferenceSection(ReferenceType.EQUIPEMENT, isEditing, refs) } },
                { SheetCard("Compétences") { LinkedReferenceSection(ReferenceType.COMPETENCE, isEditing, refs) } },
            ),
        )
        SheetCard("Notes") { LongTextBody(isEditing, form.notes, sheet.notes) { form.notes = it } }
    }
}
