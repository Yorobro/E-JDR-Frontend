package eu.ejdr.infrastructure.config

import android.content.Context
import java.io.File

/** Dossier de données Android : le stockage interne privé de l'application. */
fun provideDataDir(context: Context): File = context.filesDir
