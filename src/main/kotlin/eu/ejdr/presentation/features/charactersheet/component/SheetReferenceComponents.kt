package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Données de référence fournies à la fiche détail : catalogues N‑1 (formations/peuples) pour les
 * dropdowns, et — par type liable — les éléments rattachés + le catalogue de l'utilisateur, plus
 * les actions de rattachement/détachement. Regroupées dans un seul objet pour ne pas exploser les
 * signatures des onglets/sections.
 *
 * @property formations Catalogue des formations de l'utilisateur.
 * @property peoples Catalogue des peuples de l'utilisateur.
 * @property linked Éléments rattachés à la fiche, par type liable.
 * @property catalogues Catalogues de l'utilisateur, par type liable (pour le dialog d'ajout).
 * @property onLink Rattache un élément (type, itemId).
 * @property onUnlink Détache un élément (type, itemId).
 */
class SheetReferences(
    val formations: List<ReferenceItem>,
    val peoples: List<ReferenceItem>,
    val linked: Map<ReferenceType, List<ReferenceItem>>,
    val catalogues: Map<ReferenceType, List<ReferenceItem>>,
    val onLink: (ReferenceType, String) -> Unit,
    val onUnlink: (ReferenceType, String) -> Unit,
)

/** Libellé d'affichage « — » pour « aucun choix ». */
private const val NONE_LABEL = "—"

/**
 * Cellule de référence N‑1 (formation/peuple) : `AppDropdown` en édition (options = noms du
 * catalogue + « — » pour vider), valeur lue résolue id→nom en lecture.
 *
 * @param label Libellé du champ (« Formation », « Peuple »).
 * @param editing Mode édition.
 * @param selectedId Id actuellement sélectionné (vide si aucun).
 * @param catalogue Catalogue dans lequel choisir et résoudre l'id en nom.
 * @param onSelectId Callback portant le nouvel id (vide si « — »).
 */
@Composable
fun ReferenceCell(
    label: String,
    editing: Boolean,
    selectedId: String,
    catalogue: List<ReferenceItem>,
    onSelectId: (String) -> Unit,
) {
    val selectedName = catalogue.firstOrNull { it.id == selectedId }?.name
    if (editing) {
        AppDropdown(
            value = selectedName,
            options = listOf(NONE_LABEL) + catalogue.map { it.name },
            onSelect = { name ->
                val id = if (name == NONE_LABEL) "" else catalogue.firstOrNull { it.name == name }?.id.orEmpty()
                onSelectId(id)
            },
            label = label,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        ReadCellPublic(label, selectedName)
    }
}

/**
 * Section N‑N (armes/armures/compétences/équipements) : liste des éléments rattachés en cartes
 * (avec retrait en édition) + bouton « Ajouter » (en édition) ouvrant le [ReferencePickerDialog].
 *
 * @param type Type liable géré par la section.
 * @param editing Mode édition (affiche retrait + bouton d'ajout).
 * @param refs Données de référence (liaisons, catalogue, actions).
 */
@Composable
fun LinkedReferenceSection(
    type: ReferenceType,
    editing: Boolean,
    refs: SheetReferences,
) {
    var showPicker by remember(type) { mutableStateOf(false) }
    val linked = refs.linked[type].orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        if (linked.isEmpty()) {
            AppText(text = NONE_LABEL, style = AppTextStyle.Body, color = AppTheme.colors.muted)
        } else {
            linked.forEach { item ->
                LinkedReferenceCard(
                    name = item.name,
                    onRemove = if (editing) ({ refs.onUnlink(type, item.id) }) else null,
                )
            }
        }
        if (editing) {
            AppButton(
                label = "Ajouter une ${type.singularLabel}",
                onClick = { showPicker = true },
                variant = ButtonVariant.Secondary,
                leadingIcon = Icons.Filled.Add,
            )
        }
    }

    if (showPicker) {
        // On ne propose que les éléments du catalogue pas encore rattachés.
        val linkedIds = linked.map { it.id }.toSet()
        ReferencePickerDialog(
            type = type,
            options = refs.catalogues[type].orEmpty().filter { it.id !in linkedIds },
            onSelect = { itemId ->
                showPicker = false
                refs.onLink(type, itemId)
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Section Compétences : les compétences manuelles N‑N (gérées comme avant via [LinkedReferenceSection])
 * PLUS, en lecture seule, les compétences apportées par la formation active — badge « via <formation> »,
 * sans croix de retrait (elles ne sont pas retirables individuellement). Si la fiche n'a pas de
 * formation (ou pas de compétences dérivées), seules les compétences manuelles sont affichées.
 *
 * @param editing Mode édition (transmis à la sous-section N‑N).
 * @param refs Données de référence (liaisons N‑N, catalogue, actions).
 * @param formation Formation résolue de la fiche (source des compétences dérivées), ou `null`.
 */
@Composable
fun CompetencesSection(
    editing: Boolean,
    refs: SheetReferences,
    formation: ResolvedFormation?,
) {
    val sourceName = formation?.name
    val derived = formation?.competences.orEmpty()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        LinkedReferenceSection(ReferenceType.COMPETENCE, editing, refs)
        if (sourceName != null) {
            derived.forEach { competence ->
                DerivedCompetenceCard(name = competence.name, source = sourceName)
            }
        }
    }
}

/** Carte d'une compétence dérivée (lecture seule) : nom + badge « via <formation> ». */
@Composable
private fun DerivedCompetenceCard(name: String, source: String) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.beige)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .padding(horizontal = AppTheme.dimens.md, vertical = AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = name, style = AppTextStyle.Body)
        AppText(
            text = "via $source",
            style = AppTextStyle.Caption,
            color = AppTheme.colors.textSecondary,
        )
    }
}

/** Carte d'un élément rattaché : nom + croix de retrait optionnelle (édition). */
@Composable
private fun LinkedReferenceCard(name: String, onRemove: (() -> Unit)?) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .padding(horizontal = AppTheme.dimens.md, vertical = AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = name, style = AppTextStyle.Body)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                AppIcon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Retirer",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}

/**
 * Dialog de sélection d'un élément du catalogue à rattacher (clone du LinkCharacterDialog).
 *
 * @param type Type concerné (pour le titre).
 * @param options Éléments rattachables (catalogue moins les déjà liés).
 * @param onSelect Callback portant l'id choisi.
 * @param onDismiss Fermeture sans sélection.
 */
@Composable
fun ReferencePickerDialog(
    type: ReferenceType,
    options: List<ReferenceItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Ajouter une ${type.singularLabel}", style = AppTextStyle.Title) },
        text = {
            if (options.isEmpty()) {
                AppText(
                    text = "Aucun élément disponible. Créez-en dans « Mes éléments ».",
                    color = AppTheme.colors.muted,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(options, key = { it.id }) { item ->
                        AppText(
                            text = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.id) }
                                .padding(AppTheme.dimens.sm),
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(label = "Fermer", onClick = onDismiss, variant = ButtonVariant.Ghost)
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}
