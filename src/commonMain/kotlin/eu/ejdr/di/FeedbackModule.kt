package eu.ejdr.di

import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.infrastructure.feedback.InMemoryUiMessageBus
import org.koin.dsl.module

/** Module Koin du feedback UI : le bus de messages transitoires (singleton). */
val feedbackModule = module {
    single<UiMessageBus> { InMemoryUiMessageBus() }
}
