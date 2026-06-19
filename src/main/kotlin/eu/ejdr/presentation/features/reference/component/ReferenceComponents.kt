package eu.ejdr.presentation.features.reference.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.charactersheet.component.StatKeys
import eu.ejdr.presentation.shared.component.atomic.AppCheckbox
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppNumberField
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

private val CardHeight = 120.dp
private val CompetencePickerMaxHeight = 200.dp

/** Libellé du choix « aucune statistique » dans le dropdown. */
private const val NO_STAT_LABEL = "Aucune"

/** Bonus par défaut proposé dès qu'une statistique est choisie (le serveur applique 1 sinon). */
private const val DEFAULT_BONUS = "1"

/**
 * Options de statistique du dialog : libellé affiché ↔ slug serveur (`null` = aucune). Ordre et
 * libellés alignés sur la section caractéristiques de la fiche. Les slugs proviennent de
 * [StatKeys] (source de vérité unique) ; seul le libellé « Aucune » est propre à ce dialog.
 */
private val STAT_OPTIONS: List<Pair<String, String?>> = buildList {
    add(NO_STAT_LABEL to null)
    StatKeys.ORDERED.forEach { (slug, label) -> add(label to slug) }
}

/**
 * Tuile d'un élément de référence dans la grille de gestion (composant bête) : nom centré + icône
 * de suppression. Clone de `CampaignCard`, sans clic d'ouverture (les éléments n'ont pas de détail).
 *
 * @param item Élément à afficher.
 * @param onDelete Callback de suppression.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun ReferenceCard(
    item: ReferenceItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape),
    ) {
        AppText(
            text = item.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
            AppIcon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Supprimer",
                tint = AppTheme.colors.danger,
            )
        }
    }
}

/**
 * Dialog de création d'un élément de référence (composant bête).
 *
 * Pour [ReferenceType.FORMATION]/[ReferenceType.PEUPLE], propose en plus un sélecteur de
 * statistique et un montant de bonus (visible seulement si une stat est choisie). Pour la seule
 * formation, propose aussi un picker multi‑sélection de compétences ([availableCompetences]). Les
 * autres types n'affichent que le champ nom. La validation porte `(name, stat, bonus, competenceIds)`.
 *
 * @param type Catégorie courante (détermine les champs proposés).
 * @param title Titre du dialog (ex. « Nouvelle formation »).
 * @param label Libellé du champ nom.
 * @param onDismiss Fermeture sans création.
 * @param onConfirm Confirmation : `(name, stat slug ou null, bonus ou null, ids de compétences)`.
 * @param availableCompetences Catalogue des compétences proposées (formation uniquement).
 * @param errorMessage Message d'erreur éventuel (ex. doublon).
 */
@Composable
fun CreateReferenceDialog(
    type: ReferenceType,
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, stat: String?, bonus: Int?, competenceIds: List<String>) -> Unit,
    availableCompetences: List<ReferenceItem> = emptyList(),
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf("") }
    var statLabel by remember { mutableStateOf(NO_STAT_LABEL) }
    var bonus by remember { mutableStateOf(DEFAULT_BONUS) }
    val selectedCompetences = remember { mutableStateListOf<String>() }

    val hasStatChoice = type == ReferenceType.FORMATION || type == ReferenceType.PEUPLE
    val selectedStat = STAT_OPTIONS.firstOrNull { it.first == statLabel }?.second

    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = {
            onConfirm(
                name.trim(),
                selectedStat,
                selectedStat?.let { bonus.toIntOrNull() ?: 1 },
                selectedCompetences.toList(),
            )
        },
        confirmEnabled = name.isNotBlank(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasStatChoice) {
                StatBonusFields(
                    statLabel = statLabel,
                    onStatSelected = { newLabel ->
                        statLabel = newLabel
                        // Quand l'utilisateur choisit une stat (non-Aucune) après avoir vidé le
                        // champ bonus, on le remet à la valeur par défaut pour rester cohérent
                        // avec « défaut 1 si stat choisie ».
                        val newSlug = STAT_OPTIONS.firstOrNull { it.first == newLabel }?.second
                        if (newSlug != null && bonus.isBlank()) bonus = DEFAULT_BONUS
                    },
                    bonus = bonus,
                    onBonusChange = { bonus = it },
                    showBonus = selectedStat != null,
                )
            }
            if (type == ReferenceType.FORMATION) {
                CompetencePicker(
                    competences = availableCompetences,
                    selectedIds = selectedCompetences,
                )
            }
            FormError(message = errorMessage)
        }
    }
}

/**
 * Sous-bloc « statistique + bonus » du dialog de création (formation/peuple) : dropdown de stat et,
 * si une stat est choisie, champ numérique du montant de bonus.
 */
@Composable
private fun StatBonusFields(
    statLabel: String,
    onStatSelected: (String) -> Unit,
    bonus: String,
    onBonusChange: (String) -> Unit,
    showBonus: Boolean,
) {
    AppDropdown(
        value = statLabel,
        options = STAT_OPTIONS.map { it.first },
        onSelect = onStatSelected,
        label = "Statistique",
        modifier = Modifier.fillMaxWidth(),
    )
    if (showBonus) {
        AppNumberField(
            value = bonus,
            onValueChange = onBonusChange,
            label = "Bonus",
            allowNegative = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Sous-bloc picker de compétences (formation) : liste de cases à cocher. Coche/décoche met à jour
 * [selectedIds] en place. Message d'invite si le catalogue est vide.
 */
@Composable
private fun CompetencePicker(
    competences: List<ReferenceItem>,
    selectedIds: SnapshotStateList<String>,
) {
    AppText(text = "Compétences", style = AppTextStyle.Subtitle)
    if (competences.isEmpty()) {
        AppText(
            text = "Aucune compétence disponible. Créez-en dans « Compétences ».",
            style = AppTextStyle.Body,
            color = AppTheme.colors.muted,
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = CompetencePickerMaxHeight),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            items(competences, key = { it.id }) { competence ->
                AppCheckbox(
                    checked = competence.id in selectedIds,
                    onCheckedChange = { checked ->
                        if (checked) selectedIds.add(competence.id) else selectedIds.remove(competence.id)
                    },
                    label = competence.name,
                )
            }
        }
    }
}

/**
 * Dialog de confirmation de suppression d'un élément (action destructive).
 *
 * @param itemName Nom de l'élément (affiché dans le message).
 * @param onConfirm Confirmation de la suppression.
 * @param onDismiss Annulation.
 */
@Composable
fun ConfirmDeleteReferenceDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        title = "Supprimer l'élément",
        onDismiss = onDismiss,
        confirmLabel = "Supprimer",
        onConfirm = onConfirm,
        confirmVariant = ButtonVariant.Danger,
    ) {
        AppText("Supprimer « $itemName » ? Il sera retiré des fiches qui l'utilisent.")
    }
}
