package eu.ejdr.presentation.features.session.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

/** Motif strict d'une date `YYYY-MM-DD` (validation côté UI ; le serveur revalide). */
private val DatePattern = Regex("""\d{4}-\d{2}-\d{2}""")

/**
 * Boîte de dialogue de création d'une session (composant bête).
 *
 * Habille le modal réutilisable [AppDialog] avec un champ « titre » et un champ « date »
 * (format `AAAA-MM-JJ`). La saisie est un état d'UI local ; la validation réelle est faite
 * par le serveur et son éventuelle erreur est affichée via [errorMessage]. Le bouton de
 * confirmation reste désactivé tant que le titre est vide ou la date mal formée.
 *
 * @param onDismiss Callback de fermeture sans création.
 * @param onConfirm Callback de confirmation, portant le titre et la date saisis.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher sous les champs (ex. titre/date invalide).
 */
@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val dateValid = DatePattern.matches(date)

    AppDialog(
        title = "Nouvelle session",
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { onConfirm(title.trim(), date.trim()) },
        modifier = modifier,
        confirmEnabled = title.isNotBlank() && dateValid,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titre de la session",
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = date,
                onValueChange = { date = it },
                label = "Date (AAAA-MM-JJ)",
                placeholder = "2026-06-20",
                errorMessage = if (date.isNotBlank() && !dateValid) "Format attendu : AAAA-MM-JJ" else null,
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(message = errorMessage)
        }
    }
}
