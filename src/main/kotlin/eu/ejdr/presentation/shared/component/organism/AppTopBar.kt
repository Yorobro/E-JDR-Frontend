package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Organisme barre de navigation supérieure (top bar) de la zone connectée.
 *
 * Composant bête : affiche le titre à gauche (précédé d'un bouton retour si [onBack] est fourni)
 * et les actions à droite (icône paramètres si [onSettings] est fourni, puis déconnexion).
 *
 * @param title Titre affiché à gauche.
 * @param onLogout Callback déclenché au clic sur le bouton de déconnexion.
 * @param modifier Modifier Compose appliqué à la barre.
 * @param onSettings Callback pour ouvrir les paramètres ; si `null`, l'icône est masquée.
 * @param onBack Callback pour revenir en arrière ; si `null`, le bouton est masqué.
 */
@Composable
fun AppTopBar(
    title: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .heightIn(min = 56.dp)
            .padding(horizontal = AppTheme.dimens.lg, vertical = AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                    )
                }
            }
            AppText(text = title, style = AppTextStyle.Title)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onSettings != null) {
                IconButton(onClick = onSettings) {
                    AppIcon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Paramètres",
                    )
                }
            }
            AppButton(
                label = "Déconnexion",
                onClick = onLogout,
                variant = ButtonVariant.Text,
            )
        }
    }
}
