package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Champ de saisie numérique.
 *
 * S'appuie sur [AppTextField] en filtrant la saisie via [filterNumericInput] et en
 * proposant un clavier numérique. La valeur est gérée en `String` pour préserver les
 * états intermédiaires (ex. « 12. » pendant la frappe). Composant bête.
 *
 * @param value Valeur courante (texte).
 * @param onValueChange Callback recevant la valeur filtrée.
 * @param label Libellé du champ.
 * @param modifier Modifier Compose appliqué au champ.
 * @param errorMessage Message d'erreur ; non nul met le champ en état d'erreur.
 * @param enabled Active ou désactive la saisie.
 * @param allowDecimal Autorise les nombres décimaux.
 * @param allowNegative Autorise les nombres négatifs.
 */
@Composable
fun AppNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    allowDecimal: Boolean = false,
    allowNegative: Boolean = false,
) {
    AppTextField(
        value = value,
        onValueChange = { onValueChange(filterNumericInput(it, allowDecimal, allowNegative)) },
        label = label,
        modifier = modifier,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
