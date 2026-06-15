package eu.ejdr.infrastructure.config

import java.io.File

/**
 * Petit lecteur de fichier `.env` (sans dépendance externe).
 *
 * Format supporté, ligne par ligne :
 * - `CLE=valeur`
 * - lignes vides et commentaires (`#`) ignorés ;
 * - préfixe `export ` toléré (`export CLE=valeur`) ;
 * - guillemets simples ou doubles entourant la valeur retirés.
 *
 * Le fichier n'est jamais commité (cf. `.gitignore`). Il sert d'override **local**
 * pour pointer vers l'API de dev plutôt que celle de prod.
 */
internal object EnvFile {

    /** Charge le `.env` situé à [path] (défaut : répertoire de travail courant). */
    fun load(path: File = File(".env")): Map<String, String> {
        if (!path.isFile) return emptyMap()
        return path.readLines()
            .mapNotNull { parseLine(it) }
            .toMap()
    }

    private fun parseLine(rawLine: String): Pair<String, String>? {
        val line = rawLine.trim().removePrefix("export ").trim()
        if (line.isEmpty() || line.startsWith("#")) return null

        val separator = line.indexOf('=')
        if (separator <= 0) return null

        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim().unquote()
        if (key.isEmpty()) return null
        return key to value
    }

    private fun String.unquote(): String =
        when {
            length >= 2 && startsWith('"') && endsWith('"') -> substring(1, length - 1)
            length >= 2 && startsWith('\'') && endsWith('\'') -> substring(1, length - 1)
            else -> this
        }
}
