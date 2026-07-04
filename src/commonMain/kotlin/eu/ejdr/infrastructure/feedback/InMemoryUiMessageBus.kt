package eu.ejdr.infrastructure.feedback

import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.feedback.UiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus de messages UI en mémoire (SharedFlow). `replay = 0` : un message n'a de sens que pour
 * un hôte actuellement monté ; `extraBufferCapacity` évite de bloquer l'émetteur (emit non-suspend).
 */
class InMemoryUiMessageBus : UiMessageBus {
    private val mutableMessages =
        MutableSharedFlow<UiMessage>(replay = 0, extraBufferCapacity = 16)
    override val messages: Flow<UiMessage> = mutableMessages.asSharedFlow()

    override fun emit(message: UiMessage) {
        mutableMessages.tryEmit(message)
    }
}
