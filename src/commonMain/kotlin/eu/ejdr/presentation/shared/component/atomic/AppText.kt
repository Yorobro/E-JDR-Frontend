package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import eu.ejdr.presentation.shared.theme.AppTheme

/** Styles sémantiques disponibles pour [AppText]. */
enum class AppTextStyle { Title, Subtitle, Body, Label, Caption }

/**
 * Atome d'affichage de texte du design system.
 *
 * Applique automatiquement le style typographique et la couleur de texte du thème.
 * Composant bête : il ne fait qu'afficher la valeur reçue.
 *
 * @param text Texte à afficher.
 * @param modifier Modifier Compose appliqué au texte.
 * @param style Style typographique sémantique (défaut [AppTextStyle.Body]).
 * @param color Couleur explicite ; si `null`, utilise la couleur de texte du thème.
 * @param maxLines Nombre maximal de lignes.
 * @param textAlign Alignement horizontal du texte.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    val typo = AppTheme.typography
    val resolved: TextStyle = when (style) {
        AppTextStyle.Title -> typo.title
        AppTextStyle.Subtitle -> typo.subtitle
        AppTextStyle.Body -> typo.body
        AppTextStyle.Label -> typo.label
        AppTextStyle.Caption -> typo.caption
    }
    Text(
        text = text,
        modifier = modifier,
        style = resolved,
        color = color ?: AppTheme.colors.text,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}
