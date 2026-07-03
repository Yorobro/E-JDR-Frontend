package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Menu déroulant maison (remplace ExposedDropdownMenuBox) : un ancrage + un popup sous l'ancre.
 *
 * [anchor] reçoit un Modifier à poser sur le champ cliquable. Quand [expanded] est vrai, le
 * [content] (les items) s'affiche dans un [Popup] positionné juste sous l'ancre.
 */
@Composable
fun AppDropdownCore(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    anchor(Modifier)
    if (expanded) {
        Popup(
            popupPositionProvider = BelowAnchorPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Column(Modifier.fillMaxWidth()) { content() }
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
