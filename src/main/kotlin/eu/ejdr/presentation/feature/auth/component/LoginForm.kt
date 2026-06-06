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
