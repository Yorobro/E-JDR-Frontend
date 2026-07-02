package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val EmptyIconSize = 40.dp

/**
 * État vide accueillant : icône, titre, message et bouton d'action optionnel.
 *
 * Remplace les « Aucun X » textuels par un écran qui invite à agir. Composant bête.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(AppTheme.dimens.radiusXl))
                .background(AppTheme.colors.beige)
                .padding(AppTheme.dimens.lg),
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                size = EmptyIconSize,
            )
        }
        AppText(text = title, style = AppTextStyle.Subtitle, textAlign = TextAlign.Center)
        AppText(
            text = message,
            style = AppTextStyle.Body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            AppButton(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = AppTheme.dimens.sm),
            )
        }
    }
}
