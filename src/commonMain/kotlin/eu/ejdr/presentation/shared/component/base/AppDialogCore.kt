package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Coquille de modale maison (remplace material3.AlertDialog) : scrim sombre + carte centrée.
 *
 * Clic sur le scrim = [onDismiss]. Le clic sur la carte est absorbé (ne ferme pas).
 * Le contenu (titre, corps, boutons) est fourni par l'appelant `AppDialog`.
 * Entrée animée : fade + scale (respecte le paramètre reduced-motion via AppTheme.motion).
 */
@Composable
fun AppDialogCore(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    SideEffect { visible.targetState = true }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(AppTheme.motion.effectiveDuration(AppTheme.motion.durationMedium))) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(AppTheme.motion.effectiveDuration(AppTheme.motion.durationMedium)),
                    ),
                exit = fadeOut(),
            ) {
                // Absorbe le clic pour ne pas fermer quand on interagit avec la carte.
                Box(
                    modifier
                        .padding(AppTheme.dimens.lg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) { content() }
            }
        }
    }
}
