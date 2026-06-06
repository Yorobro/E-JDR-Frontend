package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Molécule d'affichage d'un message d'erreur de formulaire.
 *
 * Composant réutilisable et sans dépendance métier : il affiche le message reçu avec le style
 * d'erreur du thème. Si [message] est `null`, rien n'est rendu.
 *
 * @param message Message d'erreur à afficher, ou `null` pour ne rien afficher.
 * @param modifier Modifier Compose appliqué au texte.
 */
@Composable
fun FormError(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
    }
}
