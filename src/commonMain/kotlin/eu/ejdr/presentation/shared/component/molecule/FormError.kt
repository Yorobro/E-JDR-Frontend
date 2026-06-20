package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Molécule d'affichage d'un message d'erreur de formulaire.
 *
 * Composant réutilisable et sans dépendance métier : il affiche le message reçu en couleur
 * d'erreur (`danger`) du design system. Si [message] est `null`, rien n'est rendu.
 *
 * @param message Message d'erreur à afficher, ou `null` pour ne rien afficher.
 * @param modifier Modifier Compose appliqué au texte.
 */
@Composable
fun FormError(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        AppText(
            text = message,
            modifier = modifier,
            style = AppTextStyle.Caption,
            color = AppTheme.colors.danger,
        )
    }
}
