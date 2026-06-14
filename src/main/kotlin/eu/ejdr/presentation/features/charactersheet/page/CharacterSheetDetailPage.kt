package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.CharacterSheetDetailViewModel
import eu.ejdr.presentation.features.charactersheet.component.CaracteristiquesSection
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetFormState
import eu.ejdr.presentation.features.charactersheet.component.CombatSection
import eu.ejdr.presentation.features.charactersheet.component.IdentiteSection
import eu.ejdr.presentation.features.charactersheet.component.LongTextBody
import eu.ejdr.presentation.features.charactersheet.component.PurseSection
import eu.ejdr.presentation.features.charactersheet.component.ResponsiveColumns
import eu.ejdr.presentation.features.charactersheet.component.SheetCard
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page détail d'une fiche de personnage (composant INTELLIGENT).
 *
 * Charge la fiche complète par son identifiant via [CharacterSheetDetailViewModel] et l'affiche
 * par sections fidèles à la fiche papier. Un bouton « Modifier » bascule en édition (champs
 * éditables) ; « Enregistrer » persiste via le use case de mise à jour. Le rendu des sections
 * est délégué à des composants bêtes.
 *
 * @param id Identifiant de la fiche (sert au chargement).
 * @param name Nom de la fiche (affiché en titre, évite d'attendre le chargement).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CharacterSheetDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        CharacterSheetDetailViewModel(
            sheetId = id,
            getById = get<GetCharacterSheetUseCase>(),
            update = get<UpdateCharacterSheetUseCase>(),
        )
    }
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
        FormError(message = error)

        sheet?.let { loaded ->
            CharacterSheetDetailContent(
                sheet = loaded,
                isEditing = isEditing,
                isSaving = isLoading,
                onStartEdit = viewModel::startEdit,
                onCancelEdit = viewModel::cancelEdit,
                onSave = viewModel::save,
            )
        }
    }
}

/**
 * Contenu de la fiche : barre d'actions (Modifier/Annuler/Enregistrer) + sections.
 *
 * Tient l'état d'édition local ([CharacterSheetFormState]), réamorcé à chaque nouvelle [sheet]
 * (sortie d'édition, rechargement). Extrait de la page pour la garder concise.
 */
@Composable
private fun CharacterSheetDetailContent(
    sheet: CharacterSheet,
    isEditing: Boolean,
    isSaving: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (CharacterSheet) -> Unit,
) {
    val form = remember(sheet) { CharacterSheetFormState(sheet) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) {
        if (isEditing) {
            AppButton(label = "Annuler", onClick = onCancelEdit, variant = ButtonVariant.Secondary)
        } else {
            AppButton(label = "Modifier", onClick = onStartEdit, variant = ButtonVariant.Secondary)
        }

        SheetCard("Identité") { IdentiteSection(sheet, isEditing, form) }
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Caractéristiques") { CaracteristiquesSection(sheet, isEditing, form) } },
                { SheetCard("Combat") { CombatSection(sheet, isEditing, form) } },
                { SheetCard("Bourse") { PurseSection(sheet, isEditing, form) } },
            ),
        )
        TextZones(sheet = sheet, isEditing = isEditing, form = form)

        if (isEditing) {
            AppButton(
                label = "Enregistrer",
                onClick = { onSave(form.toCharacterSheet()) },
                enabled = form.isNameValid,
                loading = isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Trois rangées responsives de deux cartes de texte long (50/50) : [Armures · Armes],
 * [Compétences · Équipement], [Sorts & Miracles · Notes]. Extrait pour garder le contenu concis.
 */
@Composable
private fun TextZones(sheet: CharacterSheet, isEditing: Boolean, form: CharacterSheetFormState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) {
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Armures") { LongTextBody(isEditing, form.armures, sheet.armures) { form.armures = it } } },
                { SheetCard("Armes") { LongTextBody(isEditing, form.armes, sheet.armes) { form.armes = it } } },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Compétences") { LongTextBody(isEditing, form.competences, sheet.competences) { form.competences = it } } },
                { SheetCard("Équipement") { LongTextBody(isEditing, form.equipement, sheet.equipement) { form.equipement = it } } },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { SheetCard("Sorts & Miracles") { LongTextBody(isEditing, form.sortsEtMiracles, sheet.sortsEtMiracles) { form.sortsEtMiracles = it } } },
                { SheetCard("Notes") { LongTextBody(isEditing, form.notes, sheet.notes) { form.notes = it } } },
            ),
        )
    }
}
