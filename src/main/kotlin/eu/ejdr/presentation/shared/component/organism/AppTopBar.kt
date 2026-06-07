package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Organisme barre de navigation supérieure (top bar) de la zone connectée.
 *
 * Composant bête : il affiche le titre de l'application à gauche et un bouton de déconnexion
 * à droite, et remonte le clic via [onLogout]. Présent en haut de tous les écrans connectés
 * via [AppScaffold].
 *
 * @param title Titre affiché à gauche (nom de l'application ou de l'écran).
 * @param onLogout Callback déclenché au clic sur le bouton de déconnexion.
 * @param modifier Modifier Compose appliqué à la barre.
 */
@Composable
fun AppTopBar(
    title: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .heightIn(min = 56.dp)
            .padding(horizontal = AppTheme.dimens.lg, vertical = AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = title, style = AppTextStyle.Title)

        AppButton(
            label = "Déconnexion",
            onClick = onLogout,
            variant = ButtonVariant.Text,
        )
    }
}
