package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppNumberField
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/* ----------------------------------------------------------------------------------------- *
 * Sections d'affichage et d'édition d'une fiche, disposées en grille fidèle à la fiche papier.
 * Chaque section rend uniquement ses cellules : le titre et le cadre sont fournis par la carte
 * [SheetCard] qui l'enveloppe dans la page. Chaque cellule bascule lecture/édition via [editing].
 * Extrait de la page pour la garder courte.
 * ----------------------------------------------------------------------------------------- */

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

/**
 * Cellule de caractéristique : base éditable (comme [NumberCell]) PLUS, quand des bonus ciblent la
 * stat, leur rendu en lecture seule à droite — « +N » par source puis « = total ». Sans bonus, le
 * rendu est strictement identique à [NumberCell]. Le champ d'édition n'expose QUE la base : le PUT
 * continue d'envoyer la base seule (bonus/total purement affichés).
 *
 * @param label Libellé de la caractéristique.
 * @param editing Mode édition (la base reste éditable).
 * @param editValue Valeur de base éditée (liée au formulaire).
 * @param display Modèle d'affichage calculé (base + bonus + total).
 * @param onChange Callback de modification de la base.
 */
@Composable
fun StatCell(
    label: String,
    editing: Boolean,
    editValue: String,
    display: StatDisplay,
    onChange: (String) -> Unit,
) {
    if (!display.hasBonus) {
        NumberCell(label, editing, editValue, display.base, onChange)
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        if (editing) {
            AppNumberField(
                value = editValue,
                onValueChange = onChange,
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            StatBonusLine(prefix = "Base ${display.base ?: 0}", display = display)
        } else {
            AppText(text = label, style = AppTextStyle.Label, color = AppTheme.colors.textSecondary)
            StatBonusLine(prefix = (display.base ?: 0).toString(), display = display)
        }
    }
}

/** Ligne « base +N +N = total » en lecture seule (bonus atténués, total accentué). */
@Composable
private fun StatBonusLine(prefix: String, display: StatDisplay) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        AppText(text = prefix, style = AppTextStyle.Body)
        display.bonuses.forEach { bonus ->
            AppText(text = "+$bonus", style = AppTextStyle.Body, color = AppTheme.colors.textSecondary)
        }
        AppText(
            text = "= ${display.total}",
            style = AppTextStyle.Body,
            color = AppTheme.colors.primary,
        )
    }
}

/** Cellule sexe : menu déroulant M/F/NB en édition, libellé/valeur en lecture. */
@Composable
fun SexCell(editing: Boolean, value: String, readValue: String?, onSelect: (String) -> Unit) {
    if (editing) {
        AppDropdown(
            value = value.ifBlank { null },
            options = listOf("M", "F", "NB"),
            onSelect = onSelect,
            label = "Sexe",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        ReadCellPublic("Sexe", readValue)
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

/** Variante publique de [ReadCell] (utilisée par [SexCell] en lecture). */
@Composable
fun ReadCellPublic(label: String, value: String?) = ReadCell(label, value)

/**
 * Section Identité : une grille **régulière à 4 colonnes** (les deux lignes ont le même nombre de
 * colonnes, donc elles s'alignent verticalement), puis Apparence pleine largeur. Une cellule vide
 * complète la 2e ligne pour préserver l'alignement. Le titre et le cadre sont fournis par la carte
 * englobante.
 */
@Composable
fun IdentiteSection(
    sheet: CharacterSheet,
    editing: Boolean,
    form: CharacterSheetFormState,
    refs: SheetReferences,
) {
    FieldColumn {
        ResponsiveColumns(
            columns = listOf(
                { TextCell("Nom", editing, form.name, sheet.name) { form.name = it } },
                {
                    ReferenceCell("Formation", editing, form.formationId, refs.formations) {
                        form.formationId = it
                    }
                },
                { NumberCell("Niveau", editing, form.niveau, sheet.niveau) { form.niveau = it } },
                {
                    ReferenceCell("Peuple", editing, form.peupleId, refs.peoples) {
                        form.peupleId = it
                    }
                },
            ),
        )
        ResponsiveColumns(
            columns = listOf(
                { SexCell(editing, form.sexe, sheet.sexe) { form.sexe = it } },
                { TextCell("Taille / poids", editing, form.tailleEtPoids, sheet.tailleEtPoids) { form.tailleEtPoids = it } },
                { NumberCell("Âge", editing, form.age, sheet.age) { form.age = it } },
                {},
            ),
        )
        TextCell("Apparence", editing, form.apparence, sheet.apparence, multiline = true) {
            form.apparence = it
        }
    }
}

/**
 * Section Caractéristiques : les 5 caractéristiques en colonne. Chaque cellule affiche, en plus de
 * la base éditable, les bonus apportés par le peuple et la formation actifs (« base +N +N = total »).
 * Le calcul est purement d'affichage (cf. [statDisplay]) ; la base reste seule persistée.
 */
@Composable
fun CaracteristiquesSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    fun display(statKey: String, base: Int?) = statDisplay(statKey, base, sheet.formation, sheet.peuple)
    FieldColumn {
        StatCell("Dextérité", editing, form.dexterite, display("dexterite", sheet.dexterite)) { form.dexterite = it }
        StatCell("Intelligence", editing, form.intelligence, display("intelligence", sheet.intelligence)) { form.intelligence = it }
        StatCell("Perception", editing, form.perception, display("perception", sheet.perception)) { form.perception = it }
        StatCell("Social", editing, form.social, display("social", sheet.social)) { form.social = it }
        StatCell("Vigueur", editing, form.vigueur, display("vigueur", sheet.vigueur)) { form.vigueur = it }
    }
}

/** Section Combat : points de vie, points de magie, protection en colonne. */
@Composable
fun CombatSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    FieldColumn {
        NumberCell("Points de vie", editing, form.pointsDeVie, sheet.pointsDeVie) { form.pointsDeVie = it }
        NumberCell("Points de magie", editing, form.pointsDeMagie, sheet.pointsDeMagie) { form.pointsDeMagie = it }
        NumberCell("Protection", editing, form.protection, sheet.protection) { form.protection = it }
    }
}

/** Section Bourse : 3 champs Or/Argent/Cuivre en édition, montant normalisé en lecture. */
@Composable
fun PurseSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    if (editing) {
        FieldColumn {
            NumberCell("Or (PO)", true, form.purseGold, null) { form.purseGold = it }
            NumberCell("Argent (PA)", true, form.purseSilver, null) { form.purseSilver = it }
            NumberCell("Cuivre (PC)", true, form.purseCopper, null) { form.purseCopper = it }
        }
    } else {
        AppText(text = sheet.purse?.formatNormalized() ?: "—", style = AppTextStyle.Body)
    }
}

/**
 * Contenu d'une zone de texte long (sans titre : fourni par la carte englobante).
 *
 * @param editing Mode édition (champ) ou lecture (texte).
 * @param editValue Valeur en cours d'édition (liée à l'état du formulaire).
 * @param readValue Valeur à afficher en lecture seule (issue de la fiche).
 * @param onChange Callback de modification en mode édition.
 */
@Composable
fun LongTextBody(editing: Boolean, editValue: String, readValue: String?, onChange: (String) -> Unit) {
    if (editing) {
        AppTextField(
            value = editValue,
            onValueChange = onChange,
            label = "",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        AppText(text = readValue?.ifBlank { null } ?: "—", style = AppTextStyle.Body)
    }
}
