package eu.ejdr.presentation.feature.auth.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.LabeledTextField

/**
 * Formulaire de connexion.
 *
 * Composant BÊTE (stateless) : il ne détient aucun état et n'appelle aucun use case. Il reçoit
 * toutes ses valeurs en paramètres et remonte les événements (saisie, soumission, navigation)
 * via des callbacks. Toute la logique est portée par la page parente ([LoginPage]).
 *
 * @param email Valeur courante du champ email.
 * @param password Valeur courante du champ mot de passe.
 * @param errorMessage Message d'erreur à afficher, ou `null` si aucune erreur.
 * @param loading Indique si une opération est en cours (désactive les champs et boutons).
 * @param onEmailChange Callback remontant la modification de l'email.
 * @param onPasswordChange Callback remontant la modification du mot de passe.
 * @param onSubmit Callback déclenché à la soumission du formulaire.
 * @param onGoToRegister Callback de navigation vers l'écran d'inscription.
 * @param modifier Modifier Compose appliqué au formulaire.
 */
@Composable
fun LoginForm(
    email: String,
    password: String,
    errorMessage: String?,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabeledTextField(email, onEmailChange, "Email", enabled = !loading)
        LabeledTextField(password, onPasswordChange, "Mot de passe", isPassword = true, enabled = !loading)
        FormError(errorMessage)
        AppButton("Se connecter", onSubmit, loading = loading, modifier = Modifier.fillMaxWidth())
        AppButton("Créer un compte", onGoToRegister, enabled = !loading, modifier = Modifier.fillMaxWidth())
    }
}
