package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppTextField

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isPassword = isPassword,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
