package eu.ejdr

import android.app.Application
import eu.ejdr.di.initKoinAndroid

/** Application Android : initialise le conteneur Koin au démarrage du process. */
class EjdrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
