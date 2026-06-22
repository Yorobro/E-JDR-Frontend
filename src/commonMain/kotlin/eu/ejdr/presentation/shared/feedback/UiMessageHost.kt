package eu.ejdr.presentation.shared.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.presentation.shared.component.organism.AppSnackbar
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.delay

private const val SNACKBAR_VISIBLE_MS = 3000L

/**
 * Hôte global du feedback : observe [bus], affiche le dernier message en snackbar animé
 * (slide+fade depuis le bas), auto-dismiss. Le suivant remplace le courant.
 */
@Composable
fun UiMessageHost(bus: UiMessageBus, modifier: Modifier = Modifier) {
    val motion = AppTheme.motion
    var current by remember { mutableStateOf<UiMessage?>(null) }

    LaunchedEffect(bus) {
        bus.messages.collect { current = it }
    }
    LaunchedEffect(current) {
        if (current != null) {
            delay(SNACKBAR_VISIBLE_MS)
            current = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val msg = current
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(tween(motion.effectiveDuration(motion.durationMedium))) { it } +
                fadeIn(tween(motion.effectiveDuration(motion.durationMedium))),
            exit = slideOutVertically(tween(motion.effectiveDuration(motion.durationMedium))) { it } +
                fadeOut(tween(motion.effectiveDuration(motion.durationMedium))),
            modifier = Modifier.align(Alignment.BottomCenter).padding(AppTheme.dimens.lg),
        ) {
            if (msg != null) AppSnackbar(message = msg)
        }
    }
}
