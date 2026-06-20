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
import androidx.compose.material.icons.filled.Edit
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

/** Points de protection par défaut proposés à la création d'une armure. */
private const val DEFAULT_PROTECTION = "0"

/**
 * Options de statistique du dialog : libellé affiché ↔ slug serveur (`null` = aucune). Ordre et
 * libellés alignés sur la section caractéristiques de la fiche. Les slugs proviennent de
 * [StatKeys] (source de vérité unique) ; seul le libellé « Aucune » est propre à ce dialog.
 */
private val STAT_OPTIONS: List<Pair<String, String?>> = buildList {
    add(NO_STAT_LABEL to null)
    StatKeys.ORDERED.forEach { (slug, label) -> add(label to slug) }
}

/** Libellé de stat pré-sélectionné dans le dropdown pour [initial] (ou « Aucune » à la création). */
private fun initialStatLabel(initial: ReferenceItem?): String =
    STAT_OPTIONS.firstOrNull { it.second == initial?.stat }?.first ?: NO_STAT_LABEL

/**
 * Tuile d'un élément de référence dans la grille de gestion (composant bête) : nom centré, contenu
 * spécifique au [type] sous le nom, et icônes éditer/supprimer. Clone de `CampaignCard`, sans clic
 * d'ouverture (les éléments n'ont pas de détail).
 *
 * Le contenu sous le nom dépend du [type] :
 * - armure : points de protection ;
 * - formation : stat/bonus (si présent) + noms des compétences liées (résolus via [competenceNames]) ;
 * - peuple : stat/bonus (si présent) ;
 * - autres : rien (nom seul).
 *
 * @param item Élément à afficher.
 * @param type Catégorie de l'élément (détermine le contenu affiché sous le nom).
 * @param onEdit Callback de modification.
 * @param onDelete Callback de suppression.
 * @param modifier Modifier Compose appliqué à la tuile.
 * @param competenceNames Index `id → nom` des compétences (formation uniquement ; vide sinon).
 */
@Composable
fun ReferenceCard(
    item: ReferenceItem,
    type: ReferenceType,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    competenceNames: Map<String, String> = emptyMap(),
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
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
        ) {
            AppText(
                text = item.name,
                style = AppTextStyle.Subtitle,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            ReferenceCardDetails(item = item, type = type, competenceNames = competenceNames)
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopStart)) {
                AppIcon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Modifier",
                    tint = AppTheme.colors.primary,
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                AppIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}

/**
 * Détail affiché sous le nom d'une carte, selon le [type] (cf. [ReferenceCard]). Composant privé
 * extrait pour garder [ReferenceCard] court et lisible.
 */
@Composable
private fun ReferenceCardDetails(
    item: ReferenceItem,
    type: ReferenceType,
    competenceNames: Map<String, String>,
) {
    when (type) {
        ReferenceType.ARMURE ->
            CardDetailLine("Protection : ${item.protectionPoints ?: 0} pt")

        ReferenceType.PEUPLE -> StatLine(item)

        ReferenceType.FORMATION -> {
            StatLine(item)
            val names = item.competenceIds.mapNotNull { competenceNames[it] }
            if (names.isNotEmpty()) {
                CardDetailLine("Compétences : ${names.joinToString(", ")}")
            }
        }

        ReferenceType.SORT, ReferenceType.MIRACLE -> {
            val description = item.description?.takeIf { it.isNotBlank() }
            if (description != null) {
                CardDetailLine(description)
            }
        }

        ReferenceType.ARME, ReferenceType.COMPETENCE, ReferenceType.EQUIPEMENT -> Unit
    }
}

/** Ligne « Stat : {libellé FR} (+{bonus}) » d'une carte formation/peuple, si une stat est définie. */
@Composable
private fun StatLine(item: ReferenceItem) {
    val stat = item.stat ?: return
    val label = StatKeys.ORDERED.firstOrNull { it.first == stat }?.second ?: stat
    CardDetailLine("Stat : $label (+${item.bonus ?: 0})")
}

/** Ligne de détail discrète (texte secondaire centré) d'une carte de référence. */
@Composable
private fun CardDetailLine(text: String) {
    AppText(
        text = text,
        style = AppTextStyle.Body,
        color = AppTheme.colors.muted,
        maxLines = 2,
        textAlign = TextAlign.Center,
    )
}

/**
 * Dialog de création **ou** de modification d'un élément de référence (composant bête).
 *
 * Quand [initial] est `null` le dialog crée un élément (champs vides) ; sinon il modifie [initial]
 * (champs pré-remplis avec ses valeurs : nom, stat/bonus, compétences cochées, protection) et le
 * `onConfirm` porte alors un **remplacement complet**.
 *
 * Pour [ReferenceType.FORMATION]/[ReferenceType.PEUPLE], propose en plus un sélecteur de
 * statistique et un montant de bonus (visible seulement si une stat est choisie). Pour la seule
 * formation, propose aussi un picker multi‑sélection de compétences ([availableCompetences]). Pour
 * la seule [ReferenceType.ARMURE], propose un champ « Points de protection » (défaut 0, optionnel).
 * Pour [ReferenceType.SORT]/[ReferenceType.MIRACLE], propose un champ « Description » (texte libre,
 * optionnel). Les autres types n'affichent que le champ nom. La validation porte
 * `(name, stat, bonus, competenceIds, protectionPoints, description)`.
 *
 * @param type Catégorie courante (détermine les champs proposés).
 * @param title Titre du dialog (ex. « Nouvelle formation »).
 * @param label Libellé du champ nom.
 * @param confirmLabel Libellé du bouton de confirmation (ex. « Créer » / « Enregistrer »).
 * @param onDismiss Fermeture sans enregistrement.
 * @param onConfirm Confirmation :
 *   `(name, stat slug ou null, bonus ou null, ids de compétences, points de protection ou null,
 *   description ou null)`.
 * @param initial Élément à éditer (champs pré-remplis), ou `null` pour une création.
 * @param availableCompetences Catalogue des compétences proposées (formation uniquement).
 * @param errorMessage Message d'erreur éventuel (ex. doublon).
 */
@Composable
fun ReferenceFormDialog(
    type: ReferenceType,
    title: String,
    label: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        stat: String?,
        bonus: Int?,
        competenceIds: List<String>,
        protectionPoints: Int?,
        description: String?,
    ) -> Unit,
    initial: ReferenceItem? = null,
    availableCompetences: List<ReferenceItem> = emptyList(),
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var statLabel by remember { mutableStateOf(initialStatLabel(initial)) }
    var bonus by remember { mutableStateOf(initial?.bonus?.toString() ?: DEFAULT_BONUS) }
    var protection by remember {
        mutableStateOf(initial?.protectionPoints?.toString() ?: DEFAULT_PROTECTION)
    }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    val selectedCompetences = remember { mutableStateListOf<String>().apply { addAll(initial?.competenceIds.orEmpty()) } }

    val hasStatChoice = type == ReferenceType.FORMATION || type == ReferenceType.PEUPLE
    val hasDescription = type == ReferenceType.SORT || type == ReferenceType.MIRACLE
    val selectedStat = STAT_OPTIONS.firstOrNull { it.first == statLabel }?.second

    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = {
            onConfirm(
                name.trim(),
                selectedStat,
                selectedStat?.let { bonus.toIntOrNull() ?: 1 },
                selectedCompetences.toList(),
                if (type == ReferenceType.ARMURE) protection.toIntOrNull() ?: 0 else null,
                if (hasDescription) description.trim().ifBlank { null } else null,
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
            if (type == ReferenceType.ARMURE) {
                AppNumberField(
                    value = protection,
                    onValueChange = { protection = it },
                    label = "Points de protection",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (hasDescription) {
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
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
