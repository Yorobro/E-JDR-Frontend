package eu.ejdr.application.shared.feedback

import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.Flow

/**
 * Bus applicatif des messages UI transitoires : les ViewModels y publient les retours
 * d'action (succès/erreur), un hôte global les observe pour afficher un snackbar.
 */
interface UiMessageBus {
    /** Flux des messages (chaud : seuls les abonnés au moment de l'émission reçoivent). */
    val messages: Flow<UiMessage>

    /** Publie un message à destination de l'hôte de snackbar. Non bloquant. */
    fun emit(message: UiMessage)
}
