package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import eu.ejdr.presentation.navigation.NavItem
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Barre de navigation principale Android (icône seule, libellé uniquement sur l'onglet actif).
 *
 * L'onglet actif est matérialisé par une **pastille dorée** ([AppTheme.colors.beige]) contenant
 * l'icône teintée en [AppTheme.colors.primary] et le libellé de la destination.
 * Les onglets inactifs n'affichent qu'une icône en [AppTheme.colors.textSecondary].
 *
 * @param items Liste des entrées de navigation à afficher.
 * @param currentRoute Destination courante du back-stack (null = aucune).
 * @param onSelect Callback déclenché avec la [Route] sélectionnée.
 * @param modifier Modifier Compose appliqué à la barre.
 */
@Composable
fun AppBottomBar(
    items: List<NavItem>,
    currentRoute: NavKey?,
    onSelect: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppTheme.dimens
    AppSurface(
        color = AppTheme.colors.surface,
        elevation = dimens.elevationMd,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.sm),
            horizontalArrangement = spacedBy(dimens.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute?.let { it::class == item.route::class } ?: false
                if (selected) {
                    AppSurface(
                        color = AppTheme.colors.beige,
                        shape = RoundedCornerShape(dimens.radiusLg),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = spacedBy(dimens.xs),
                            modifier = Modifier.padding(horizontal = dimens.md, vertical = dimens.xs),
                        ) {
                            AppIcon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                            )
                            AppText(
                                text = item.label,
                                style = AppTextStyle.Label,
                                color = AppTheme.colors.primary,
                            )
                        }
                    }
                } else {
                    AppIcon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .padding(horizontal = dimens.sm, vertical = dimens.xs)
                            .clickable { onSelect(item.route) },
                    )
                }
            }
        }
    }
}
