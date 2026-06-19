package eu.ejdr.di

import eu.ejdr.application.features.reference.abstraction.repository.ReferenceRepository
import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UpdateReferenceItemUseCase
import eu.ejdr.application.features.reference.usecase.CreateReferenceItemUseCaseImpl
import eu.ejdr.application.features.reference.usecase.DeleteReferenceItemUseCaseImpl
import eu.ejdr.application.features.reference.usecase.LinkSheetReferenceUseCaseImpl
import eu.ejdr.application.features.reference.usecase.ListReferenceItemsUseCaseImpl
import eu.ejdr.application.features.reference.usecase.ListSheetReferencesUseCaseImpl
import eu.ejdr.application.features.reference.usecase.UnlinkSheetReferenceUseCaseImpl
import eu.ejdr.application.features.reference.usecase.UpdateReferenceItemUseCaseImpl
import eu.ejdr.infrastructure.http.features.reference.ReferenceHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature éléments de référence : port application (use cases) + adaptateur
 * infrastructure (repository HTTP). Le `HttpClient` et l'`AppConfig` viennent du socle transverse
 * [infrastructureModule].
 */
val referenceModule = module {
    single<ReferenceRepository> { ReferenceHttpRepository(get(), get()) }
    single<ListReferenceItemsUseCase> { ListReferenceItemsUseCaseImpl(get()) }
    single<CreateReferenceItemUseCase> { CreateReferenceItemUseCaseImpl(get()) }
    single<UpdateReferenceItemUseCase> { UpdateReferenceItemUseCaseImpl(get()) }
    single<DeleteReferenceItemUseCase> { DeleteReferenceItemUseCaseImpl(get()) }
    single<ListSheetReferencesUseCase> { ListSheetReferencesUseCaseImpl(get()) }
    single<LinkSheetReferenceUseCase> { LinkSheetReferenceUseCaseImpl(get()) }
    single<UnlinkSheetReferenceUseCase> { UnlinkSheetReferenceUseCaseImpl(get()) }
}
