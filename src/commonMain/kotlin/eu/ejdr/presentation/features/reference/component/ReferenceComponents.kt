package eu.ejdr.presentation.features.reference.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import eu.ejdr.application.features.reference.abstraction.ReferenceItemForm
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.charactersheet.component.StatKeys
import eu.ejdr.presentation.shared.component.base.AppIconButton
import eu.ejdr.presentation.shared.component.atomic.AppCheckbox
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppNumberField
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.component.organism.AppCard
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
    AppCard(
        modifier = modifier.height(CardHeight),
        onClick = null,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
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
                AppIconButton(
                    onClick = onEdit,
                    contentDescription = "Modifier",
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    AppIcon(
                        imageVector = AppIcons.Edit,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                    )
                }
            }
            if (onDelete != null) {
                AppIconButton(
                    onClick = onDelete,
                    contentDescription = "Supprimer",
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    AppIcon(
                        imageVector = AppIcons.Delete,
                        contentDescription = null,
                        tint = AppTheme.colors.danger,
                    )
                }
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

        ReferenceType.PEUPLE -> StatLines(item)

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

/** Ligne « Stat : {libellé FR} (+{bonus}) » d'une carte **formation** (mono-bonus). */
@Composable
private fun StatLine(item: ReferenceItem) {
    val stat = item.stat ?: return
    CardDetailLine("Stat : ${statLabel(stat)} (+${item.bonus ?: 0})")
}

/**
 * Bonus d'une carte **peuple** (0..N), joints sur **une seule ligne** :
 * « Stats : Social (+2), Vigueur (+1) ».
 *
 * Une ligne par bonus déborderait : la carte fait [CardHeight] et [CardDetailLine] est limité à
 * deux lignes — un peuple peut porter jusqu'à 5 bonus.
 */
@Composable
private fun StatLines(item: ReferenceItem) {
    if (item.statBonuses.isEmpty()) return
    val text = item.statBonuses.joinToString(", ") { "${statLabel(it.stat)} (+${it.bonus})" }
    CardDetailLine("Stats : $text")
}

/** Libellé FR d'un slug de statistique (repli sur le slug si inconnu). */
private fun statLabel(slug: String): String =
    StatKeys.ORDERED.firstOrNull { it.first == slug }?.second ?: slug

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
    onConfirm: (ReferenceItemForm) -> Unit,
    initial: ReferenceItem? = null,
    availableCompetences: List<ReferenceItem> = emptyList(),
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nameTouched by remember { mutableStateOf(false) }
    var statLabel by remember { mutableStateOf(initialStatLabel(initial)) }
    var bonus by remember { mutableStateOf(initial?.bonus?.toString() ?: DEFAULT_BONUS) }
    var protection by remember {
        mutableStateOf(initial?.protectionPoints?.toString() ?: DEFAULT_PROTECTION)
    }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    val selectedCompetences = remember { mutableStateListOf<String>().apply { addAll(initial?.competenceIds.orEmpty()) } }
    // Lignes de bonus d'un peuple. Immuables et remplacées par `rows[i] = rows[i].copy(...)` :
    // muter un champ `var` d'une data class dans un SnapshotStateList ne déclencherait PAS la
    // recomposition (piège Compose classique).
    val statBonusRows = remember {
        mutableStateListOf<StatBonusRow>().apply {
            addAll(initial?.statBonuses.orEmpty().map { StatBonusRow(it.stat, it.bonus.toString()) })
        }
    }

    // Contrat asymétrique du backend : une formation porte AU PLUS UN bonus, un peuple 0..N.
    val hasSingleStat = type == ReferenceType.FORMATION
    val hasMultiStats = type == ReferenceType.PEUPLE
    val hasDescription = type == ReferenceType.SORT || type == ReferenceType.MIRACLE
    val selectedStat = STAT_OPTIONS.firstOrNull { it.first == statLabel }?.second

    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = {
            onConfirm(
                ReferenceItemForm(
                    name = name.trim(),
                    stat = if (hasSingleStat) selectedStat else null,
                    bonus = if (hasSingleStat) selectedStat?.let { bonus.toIntOrNull() ?: 1 } else null,
                    statBonuses = if (hasMultiStats) statBonusRows.toReferenceStatBonuses() else emptyList(),
                    competenceIds = selectedCompetences.toList(),
                    protectionPoints = if (type == ReferenceType.ARMURE) protection.toIntOrNull() ?: 0 else null,
                    description = if (hasDescription) description.trim().ifBlank { null } else null,
                ),
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
                onValueChange = { name = it; nameTouched = true },
                label = label,
                errorMessage = if (nameTouched && name.isBlank()) "Le nom ne peut pas être vide" else null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasSingleStat) {
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
            if (hasMultiStats) {
                StatBonusListFields(rows = statBonusRows)
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
 * Sous-bloc « statistique + bonus » du dialog d'une **formation** (mono-bonus) : dropdown de stat
 * et, si une stat est choisie, champ numérique du montant de bonus.
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
 * Une ligne de bonus en cours de saisie (dialog **peuple**). Immuable : dans un `SnapshotStateList`,
 * seule la **substitution** d'un élément déclenche la recomposition — muter un `var` interne
 * passerait inaperçu.
 *
 * @property statSlug Slug serveur de la statistique ciblée.
 * @property bonus Montant saisi (texte : le champ peut être transitoirement vide).
 */
private data class StatBonusRow(val statSlug: String, val bonus: String)

/** Convertit les lignes saisies en bonus du domaine (un champ vide retombe sur le défaut 1). */
private fun List<StatBonusRow>.toReferenceStatBonuses(): List<ReferenceStatBonus> =
    map { ReferenceStatBonus(stat = it.statSlug, bonus = it.bonus.toIntOrNull() ?: 1) }
        // Ceinture : le dropdown empêche déjà de choisir deux fois la même stat, et le backend
        // renverrait 400. Ce distinctBy garantit qu'on ne l'atteint jamais.
        .distinctBy { it.stat }

/**
 * Sous-bloc « bonus multiples » du dialog d'un **peuple** : une ligne par bonus (stat + montant +
 * bouton de retrait), et un bouton d'ajout.
 *
 * **Le doublon de statistique est impossible par construction** : le dropdown d'une ligne ne propose
 * que les stats **non prises par les autres lignes**. Plutôt qu'un message d'erreur a posteriori,
 * l'utilisateur ne peut simplement pas se tromper. Le bouton d'ajout disparaît quand les 5 stats
 * sont utilisées.
 */
@Composable
private fun StatBonusListFields(rows: SnapshotStateList<StatBonusRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
        AppText(text = "Bonus de caractéristique", style = AppTextStyle.Label)

        rows.forEachIndexed { index, row ->
            // Stats encore libres pour CETTE ligne : toutes sauf celles prises par les AUTRES.
            val takenByOthers = rows.filterIndexed { i, _ -> i != index }.map { it.statSlug }.toSet()
            val options = StatKeys.ORDERED.filter { it.first !in takenByOthers }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppDropdown(
                    value = statLabel(row.statSlug),
                    options = options.map { it.second },
                    onSelect = { label ->
                        val slug = StatKeys.ORDERED.firstOrNull { it.second == label }?.first
                        if (slug != null) rows[index] = row.copy(statSlug = slug)
                    },
                    label = "Statistique",
                    modifier = Modifier.weight(1f),
                )
                AppNumberField(
                    value = row.bonus,
                    onValueChange = { rows[index] = row.copy(bonus = it) },
                    label = "Bonus",
                    allowNegative = true,
                    modifier = Modifier.weight(1f),
                )
                AppIconButton(
                    onClick = { rows.removeAt(index) },
                    contentDescription = "Retirer ce bonus",
                ) {
                    AppIcon(
                        imageVector = AppIcons.Delete,
                        contentDescription = null,
                        tint = AppTheme.colors.danger,
                    )
                }
            }
        }

        val remaining = StatKeys.ORDERED.map { it.first } - rows.map { it.statSlug }.toSet()
        if (remaining.isNotEmpty()) {
            AppButton(
                label = "Ajouter un bonus",
                // Pré-remplie avec la première stat libre : une ligne est toujours valide dès sa
                // création (pas d'état « stat non choisie » à gérer).
                onClick = { rows.add(StatBonusRow(remaining.first(), DEFAULT_BONUS)) },
                variant = ButtonVariant.Secondary,
                leadingIcon = AppIcons.Add,
            )
        }
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
