package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppNumberField
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/* ----------------------------------------------------------------------------------------- *
 * Sections d'affichage et d'édition d'une fiche, disposées en grille fidèle à la fiche papier
 * (identité multi-colonnes ; bloc caractéristiques à 3 colonnes ; zones de texte pleine largeur).
 * La grille se replie en pile sur fenêtre étroite (cf. [ResponsiveColumns]). Chaque cellule
 * bascule lecture/édition via [editing]. Extrait de la page pour la garder courte.
 * ----------------------------------------------------------------------------------------- */

/** Titre d'une section de la fiche. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    AppText(text = title, style = AppTextStyle.Subtitle, modifier = modifier)
}

/**
 * Cellule de champ texte court : libellé + valeur (lecture) ou champ (édition).
 *
 * @param label Libellé du champ.
 * @param editing Mode édition.
 * @param editValue Valeur éditée (liée au formulaire).
 * @param readValue Valeur affichée en lecture.
 * @param multiline Autorise plusieurs lignes (apparence, etc.).
 * @param onChange Callback de modification.
 */
@Composable
fun TextCell(
    label: String,
    editing: Boolean,
    editValue: String,
    readValue: String?,
    multiline: Boolean = false,
    onChange: (String) -> Unit,
) {
    if (editing) {
        AppTextField(
            value = editValue,
            onValueChange = onChange,
            label = label,
            singleLine = !multiline,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        ReadCell(label, readValue)
    }
}

/** Cellule de champ numérique : libellé + valeur (lecture) ou champ nombre (édition). */
@Composable
fun NumberCell(
    label: String,
    editing: Boolean,
    editValue: String,
    readValue: Int?,
    onChange: (String) -> Unit,
) {
    if (editing) {
        AppNumberField(
            value = editValue,
            onValueChange = onChange,
            label = label,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        ReadCell(label, readValue?.toString())
    }
}

/** Affichage lecture seule d'une cellule : libellé atténué au-dessus de la valeur (ou « — »). */
@Composable
private fun ReadCell(label: String, value: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        AppText(text = label, style = AppTextStyle.Label, color = AppTheme.colors.textSecondary)
        AppText(text = value?.ifBlank { null } ?: "—", style = AppTextStyle.Body)
    }
}

/** Colonne de section : un titre suivi de son contenu, espacés. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        SectionTitle(title)
        content()
    }
}

/**
 * Section Identité, disposée comme la fiche : Nom/Formation/Niveau en ligne, puis
 * Peuple/Sexe/Taille-poids/Âge en ligne, puis Apparence pleine largeur.
 */
@Composable
fun IdentiteSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    Section("Identité") {
        ResponsiveColumns(
            columns = listOf(
                { TextCell("Nom", editing, form.name, sheet.name) { form.name = it } },
                { TextCell("Formation", editing, form.formation, sheet.formation) { form.formation = it } },
                { TextCell("Niveau", editing, form.niveau, sheet.niveau) { form.niveau = it } },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { TextCell("Peuple", editing, form.peuple, sheet.peuple) { form.peuple = it } },
                { TextCell("Sexe", editing, form.sexe, sheet.sexe) { form.sexe = it } },
                { TextCell("Taille / poids", editing, form.tailleEtPoids, sheet.tailleEtPoids) { form.tailleEtPoids = it } },
                { TextCell("Âge", editing, form.age, sheet.age) { form.age = it } },
            ),
        )
        TextCell("Apparence", editing, form.apparence, sheet.apparence, multiline = true) {
            form.apparence = it
        }
    }
}

/**
 * Section Caractéristiques, disposée comme la fiche en 3 colonnes : caractéristiques à gauche,
 * points de vie / magie / armures au milieu, protection / monnaie à droite.
 */
@Composable
fun CaracteristiquesSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    Section("Caractéristiques") {
        ResponsiveColumns(
            columns = listOf(
                { CaracteristiquesColumn(sheet, editing, form) },
                { RessourcesColumn(sheet, editing, form) },
                { DefenseColumn(sheet, editing, form) },
            ),
        )
    }
}

/** Colonne gauche : les 5 caractéristiques. */
@Composable
private fun CaracteristiquesColumn(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    FieldColumn {
        NumberCell("Dextérité", editing, form.dexterite, sheet.dexterite) { form.dexterite = it }
        NumberCell("Intelligence", editing, form.intelligence, sheet.intelligence) { form.intelligence = it }
        NumberCell("Perception", editing, form.perception, sheet.perception) { form.perception = it }
        NumberCell("Social", editing, form.social, sheet.social) { form.social = it }
        NumberCell("Vigueur", editing, form.vigueur, sheet.vigueur) { form.vigueur = it }
    }
}

/** Colonne du milieu : points de vie, points de magie, armures. */
@Composable
private fun RessourcesColumn(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    FieldColumn {
        NumberCell("Points de vie", editing, form.pointsDeVie, sheet.pointsDeVie) { form.pointsDeVie = it }
        NumberCell("Points de magie", editing, form.pointsDeMagie, sheet.pointsDeMagie) { form.pointsDeMagie = it }
        TextCell("Armures", editing, form.armures, sheet.armures, multiline = true) { form.armures = it }
    }
}

/** Colonne droite : protection, monnaie. */
@Composable
private fun DefenseColumn(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    FieldColumn {
        NumberCell("Protection", editing, form.protection, sheet.protection) { form.protection = it }
        NumberCell("Monnaie", editing, form.monnaie, sheet.monnaie) { form.monnaie = it }
    }
}

/**
 * Section générique de texte long pleine largeur (Armes, Équipement, Sorts & Miracles, Notes).
 *
 * @param title Titre de la section.
 * @param editing Mode édition (champ) ou lecture (texte).
 * @param editValue Valeur en cours d'édition (liée à l'état du formulaire).
 * @param readValue Valeur à afficher en lecture seule (issue de la fiche).
 * @param onChange Callback de modification en mode édition.
 */
@Composable
fun LongTextSection(
    title: String,
    editing: Boolean,
    editValue: String,
    readValue: String?,
    onChange: (String) -> Unit,
) {
    Section(title) {
        if (editing) {
            AppTextField(
                value = editValue,
                onValueChange = onChange,
                label = title,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppText(text = readValue?.ifBlank { null } ?: "—", style = AppTextStyle.Body)
        }
    }
}
