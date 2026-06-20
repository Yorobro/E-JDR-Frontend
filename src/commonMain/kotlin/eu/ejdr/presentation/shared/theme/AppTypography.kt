package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Styles typographiques du design system.
 *
 * Conteneur immuable lu via [AppTheme]. Chaque style correspond à un usage
 * sémantique (titre, sous-titre, corps, label, légende).
 *
 * @property title Titres d'écran.
 * @property subtitle Sous-titres / sections.
 * @property body Texte courant.
 * @property label Libellés de champs et boutons.
 * @property caption Texte secondaire (aide, erreurs courtes).
 */
data class AppTypography(
    val title: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    val subtitle: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    val body: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    val caption: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
)
