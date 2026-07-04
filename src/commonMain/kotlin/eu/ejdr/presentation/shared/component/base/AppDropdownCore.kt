package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Menu déroulant maison (remplace ExposedDropdownMenuBox) : un ancrage + un popup sous l'ancre.
 *
 * [anchor] reçoit un Modifier à poser sur le champ cliquable. Quand [expanded] est vrai, le
 * [content] (les items) s'affiche dans un [Popup] positionné juste sous l'ancre.
 *
 * Le popup **cale sa largeur sur celle de l'ancre** (comme l'ancien `ExposedDropdownMenuBox`) :
 * la largeur du champ est mesurée via `onSizeChanged` sur le Modifier de l'ancre, puis appliquée
 * au contenu. Sans ça, un `fillMaxWidth()` dans un `Popup` se mesurerait contre la fenêtre entière
 * et le menu déborderait.
 */
@Composable
fun AppDropdownCore(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    var anchorWidthPx by remember { mutableStateOf(0) }
    anchor(Modifier.onSizeChanged { anchorWidthPx = it.width })
    if (expanded) {
        val anchorWidth = with(LocalDensity.current) { anchorWidthPx.toDp() }
        Popup(
            popupPositionProvider = BelowAnchorPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Column(Modifier.width(anchorWidth)) { content() }
        }
    }
}

/** Positionne le popup juste sous l'ancre, aligné à gauche. */
private object BelowAnchorPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset = IntOffset(x = anchorBounds.left, y = anchorBounds.bottom)
}
