package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.feedback.UiMessage
import eu.ejdr.presentation.shared.feedback.UiMessageTone
import eu.ejdr.presentation.shared.theme.AppTheme

/** Bandeau transitoire de feedback (succès/erreur), couleurs dérivées du ton. */
@Composable
fun AppSnackbar(message: UiMessage, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val container = if (message.tone == UiMessageTone.ERROR) colors.danger else colors.primary
    val content = if (message.tone == UiMessageTone.ERROR) colors.onDanger else colors.onPrimary
    AppText(
        text = message.text,
        style = AppTextStyle.Body,
        color = content,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimens.radiusMd))
            .background(container)
            .padding(AppTheme.dimens.md),
    )
}
