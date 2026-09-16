package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Couleur de contenu par défaut (texte/icônes), fournie par [AppTheme].
 *
 * Remplace `androidx.compose.material3.LocalContentColor` : les atomes qui n'ont pas
 * de couleur explicite (ex. [eu.ejdr.presentation.shared.component.atomic.AppIcon])
 * héritent de cette valeur, mise à `AppColors.text` par [AppTheme].
 */
val LocalContentColor = compositionLocalOf { Color.Black }
