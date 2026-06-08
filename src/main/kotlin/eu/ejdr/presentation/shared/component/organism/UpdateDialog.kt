package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme
import java.awt.Desktop
import java.net.URI

@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Mise à jour disponible", style = AppTextStyle.Title) },
        text = { AppText("La version ${info.version} est disponible. Voulez-vous la télécharger ?") },
        confirmButton = {
            AppButton(
                label = "Télécharger",
                onClick = {
                    runCatching { Desktop.getDesktop().browse(URI(info.releaseUrl)) }
                    onDismiss()
                },
            )
        },
        dismissButton = {
            AppButton(label = "Plus tard", onClick = onDismiss, variant = ButtonVariant.Ghost)
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}
