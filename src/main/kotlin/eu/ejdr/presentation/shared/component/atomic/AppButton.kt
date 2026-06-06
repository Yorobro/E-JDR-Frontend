package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Atome bouton de l'application.
 *
 * Composant réutilisable et sans dépendance métier : il se contente d'afficher un libellé
 * et de remonter le clic. Pendant le chargement, il affiche un indicateur de progression et
 * se désactive automatiquement pour éviter les soumissions multiples.
 *
 * @param label Texte affiché sur le bouton.
 * @param onClick Callback déclenché lors du clic.
 * @param modifier Modifier Compose appliqué au bouton.
 * @param enabled Active ou désactive le bouton.
 * @param loading Si vrai, affiche un indicateur de chargement et désactive le bouton.
 */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = modifier) {
        if (loading) CircularProgressIndicator() else Text(label)
    }
}
