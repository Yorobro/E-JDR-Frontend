package eu.ejdr.presentation.shared.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Jeu d'icônes maison du design system (trait fin, style manuscrit léger).
 *
 * Remplace `androidx.compose.material.icons`. Chaque icône est un [ImageVector] 24×24 tracé
 * au trait ; la couleur est appliquée par `AppIcon` via le `tint`. Un écran de galerie interne
 * (dev) permet de tous les vérifier visuellement.
 */
object AppIcons {

    // Helper interne : construit une icône « au trait » de 24dp.
    private fun stroked(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) { block() }
            }
            .build()

    val Add: ImageVector by lazy { stroked("Add") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Close: ImageVector by lazy { stroked("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) } }
    val ArrowBack: ImageVector by lazy { stroked("ArrowBack") { moveTo(19f, 12f); lineTo(5f, 12f); moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f) } }
    val List: ImageVector by lazy { stroked("List") { moveTo(4f, 7f); lineTo(20f, 7f); moveTo(4f, 12f); lineTo(20f, 12f); moveTo(4f, 17f); lineTo(20f, 17f) } }
    val Delete: ImageVector by lazy { stroked("Delete") { moveTo(5f, 7f); lineTo(19f, 7f); moveTo(9f, 7f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(15f, 7f); moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f) } }
    val Edit: ImageVector by lazy { stroked("Edit") { moveTo(4f, 20f); lineTo(4f, 16f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(8f, 20f); close() } }
    val ContentCopy: ImageVector by lazy { stroked("ContentCopy") { moveTo(9f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); lineTo(9f, 20f); close(); moveTo(9f, 15f); lineTo(4f, 15f); lineTo(4f, 4f); lineTo(15f, 4f); lineTo(15f, 9f) } }
    val Person: ImageVector by lazy { stroked("Person") { moveTo(12f, 4f); arcToRelative(4f, 4f, 0f, true, true, 0f, 8f); arcToRelative(4f, 4f, 0f, true, true, 0f, -8f); close(); moveTo(4f, 21f); arcToRelative(8f, 8f, 0f, false, true, 16f, 0f) } }
    val PersonOutline: ImageVector by lazy { Person }
    val AccountCircle: ImageVector by lazy { stroked("AccountCircle") { moveTo(12f, 3f); arcToRelative(9f, 9f, 0f, true, true, 0f, 18f); arcToRelative(9f, 9f, 0f, true, true, 0f, -18f); close(); moveTo(12f, 8f); arcToRelative(2.5f, 2.5f, 0f, true, true, 0f, 5f); arcToRelative(2.5f, 2.5f, 0f, true, true, 0f, -5f); close(); moveTo(6.5f, 18f); arcToRelative(5.5f, 5.5f, 0f, false, true, 11f, 0f) } }
    val Group: ImageVector by lazy { stroked("Group") { moveTo(9f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close(); moveTo(3f, 19f); arcToRelative(6f, 6f, 0f, false, true, 12f, 0f); moveTo(16f, 8f); arcToRelative(3f, 3f, 0f, false, true, 0f, 6f); moveTo(21f, 19f); arcToRelative(6f, 6f, 0f, false, false, -4f, -5.6f) } }
    val Groups: ImageVector by lazy { Group }
    val Home: ImageVector by lazy { stroked("Home") { moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f); moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f) } }
    val Settings: ImageVector by lazy { stroked("Settings") { moveTo(12f, 9f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close(); moveTo(12f, 2f); lineTo(12f, 5f); moveTo(12f, 19f); lineTo(12f, 22f); moveTo(2f, 12f); lineTo(5f, 12f); moveTo(19f, 12f); lineTo(22f, 12f) } }
    val Category: ImageVector by lazy { stroked("Category") { moveTo(12f, 3f); lineTo(16f, 9f); lineTo(8f, 9f); close(); moveTo(4f, 13f); lineTo(10f, 13f); lineTo(10f, 19f); lineTo(4f, 19f); close(); moveTo(17f, 13f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close() } }
    val Castle: ImageVector by lazy { stroked("Castle") { moveTo(4f, 20f); lineTo(4f, 8f); lineTo(7f, 8f); lineTo(7f, 5f); lineTo(9f, 5f); lineTo(9f, 8f); lineTo(15f, 8f); lineTo(15f, 5f); lineTo(17f, 5f); lineTo(17f, 8f); lineTo(20f, 8f); lineTo(20f, 20f); close() } }
    val MenuBook: ImageVector by lazy { stroked("MenuBook") { moveTo(12f, 6f); lineTo(12f, 20f); moveTo(12f, 6f); arcToRelative(6f, 3f, 0f, false, false, -8f, 0f); lineTo(4f, 18f); arcToRelative(6f, 3f, 0f, false, true, 8f, 0f); moveTo(12f, 6f); arcToRelative(6f, 3f, 0f, false, true, 8f, 0f); lineTo(20f, 18f); arcToRelative(6f, 3f, 0f, false, false, -8f, 0f) } }
    val Badge: ImageVector by lazy { stroked("Badge") { moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(5f, 19f); close(); moveTo(9f, 9f); lineTo(15f, 9f); moveTo(9f, 13f); lineTo(13f, 13f) } }
    val Mail: ImageVector by lazy { stroked("Mail") { moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(4f, 18f); close(); moveTo(4f, 7f); lineTo(12f, 13f); lineTo(20f, 7f) } }
    val Email: ImageVector by lazy { Mail }
    val Lock: ImageVector by lazy { stroked("Lock") { moveTo(7f, 11f); lineTo(7f, 8f); arcToRelative(5f, 5f, 0f, false, true, 10f, 0f); lineTo(17f, 11f); moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 20f); lineTo(5f, 20f); close() } }
    val Visibility: ImageVector by lazy { stroked("Visibility") { moveTo(2f, 12f); arcToRelative(10f, 6f, 0f, false, true, 20f, 0f); arcToRelative(10f, 6f, 0f, false, true, -20f, 0f); close(); moveTo(12f, 9f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close() } }
    val VisibilityOff: ImageVector by lazy { stroked("VisibilityOff") { moveTo(3f, 12f); arcToRelative(10f, 6f, 0f, false, true, 18f, -3f); moveTo(21f, 12f); arcToRelative(10f, 6f, 0f, false, true, -14f, 4.5f); moveTo(4f, 4f); lineTo(20f, 20f) } }
}
