package eu.ejdr.di

import org.koin.core.context.startKoin

fun initKoin() = startKoin {
    modules(infrastructureModule, applicationModule)
}
