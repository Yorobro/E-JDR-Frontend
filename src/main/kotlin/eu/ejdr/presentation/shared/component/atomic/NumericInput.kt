package eu.ejdr.presentation.shared.component.atomic

/**
 * Filtre une saisie brute pour ne conserver qu'un nombre valide.
 *
 * Conserve les chiffres ; selon les options, autorise un point décimal unique et
 * un signe moins en tête. Fonction pure (testable sans UI), utilisée par
 * [AppNumberField].
 *
 * @param raw Texte saisi brut.
 * @param allowDecimal Autorise un seul séparateur décimal `.`.
 * @param allowNegative Autorise un `-` en première position.
 * @return La chaîne nettoyée ne contenant qu'un nombre valide (éventuellement vide).
 */
fun filterNumericInput(raw: String, allowDecimal: Boolean, allowNegative: Boolean): String {
    val sb = StringBuilder()
    var dotUsed = false
    raw.forEachIndexed { index, c ->
        when {
            c.isDigit() -> sb.append(c)
            c == '-' && allowNegative && index == 0 -> sb.append(c)
            c == '.' && allowDecimal && !dotUsed -> {
                dotUsed = true
                sb.append(c)
            }
        }
    }
    return sb.toString()
}
