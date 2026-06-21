package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome barre d'onglets du design system (composant bête).
 *
 * Affiche une rangée d'onglets cliquables à largeur égale. L'onglet sélectionné est mis en
 * avant (texte `primary` + ligne basse `primary`) ; les autres utilisent `textSecondary`. Ne
 * tient aucun état : reçoit [selectedIndex] et remonte le clic via [onSelect].
 *
 * @param tabs Libellés des onglets, dans l'ordre.
 * @param selectedIndex Index de l'onglet actif.
 * @param onSelect Callback remontant l'index cliqué.
 * @param modifier Modifier Compose appliqué à la barre.
 */
@Composable
fun AppTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        tabs.forEachIndexed { index, label ->
            TabLabel(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Un onglet : libellé centré + ligne basse `primary` quand [selected] (tracée hors layout). */
@Composable
private fun TabLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicator = AppTheme.colors.primary
    val strokePx = with(LocalDensity.current) { AppTheme.dimens.borderWidthFocused.toPx() }
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .drawBehind {
                if (selected) {
                    drawLine(indicator, Offset(0f, size.height), Offset(size.width, size.height), strokePx)
                }
            }
            .padding(vertical = AppTheme.dimens.sm),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            style = AppTextStyle.Label,
            color = if (selected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
