package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Bulle d'aide au survol (desktop uniquement).
 *
 * Donne un nom à une icône seule de la top bar : au survol de [content], une petite bulle
 * `surface` affiche [text]. Repose sur [TooltipArea] (disponible sur la cible desktop de
 * Compose Multiplatform ; il n'existe pas d'équivalent Android, d'où le placement dans
 * `desktopMain`). Aucune dépendance Material : la bulle est un [AppSurface] maison.
 *
 * @param text Libellé affiché dans la bulle.
 * @param content Contenu survolable (généralement un bouton-icône).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            AppSurface(
                color = AppTheme.colors.surface,
                contentColor = AppTheme.colors.text,
                shape = RoundedCornerShape(AppTheme.dimens.radiusSm),
                elevation = AppTheme.dimens.elevationMd,
            ) {
                Box(
                    Modifier.padding(
                        horizontal = AppTheme.dimens.sm,
                        vertical = AppTheme.dimens.xs,
                    ),
                ) { AppText(text, style = AppTextStyle.Caption) }
            }
        },
        content = content,
    )
}
