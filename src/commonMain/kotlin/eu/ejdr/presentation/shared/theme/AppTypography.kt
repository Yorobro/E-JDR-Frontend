package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Styles typographiques du design system.
 *
 * Conteneur immuable lu via [AppTheme]. Chaque style correspond à un usage
 * sémantique (titre, sous-titre, corps, label, légende, monospace).
 *
 * @property title Titres d'écran.
 * @property subtitle Sous-titres / sections.
 * @property body Texte courant.
 * @property label Libellés de champs et boutons.
 * @property caption Texte secondaire (aide, erreurs courtes).
 * @property mono Valeurs numériques / stats (monospace).
 */
data class AppTypography(
    val title: TextStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    val subtitle: TextStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    val body: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    val label: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    val mono: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
)

/**
 * Construit un [AppTypography] avec les familles de polices custom du design system.
 *
 * - title/subtitle → Fraunces (display)
 * - body/label/caption → Inter (corps)
 * - mono → JetBrains Mono
 */
@Composable
fun appTypography(): AppTypography {
    val display = appDisplayFamily()
    val body = appBodyFamily()
    val mono = appMonoFamily()
    return AppTypography(
        title = AppTypography().title.copy(fontFamily = display),
        subtitle = AppTypography().subtitle.copy(fontFamily = display),
        body = AppTypography().body.copy(fontFamily = body),
        label = AppTypography().label.copy(fontFamily = body),
        caption = AppTypography().caption.copy(fontFamily = body),
        mono = AppTypography().mono.copy(fontFamily = mono),
    )
}
