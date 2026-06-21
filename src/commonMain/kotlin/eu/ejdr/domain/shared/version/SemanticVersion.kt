package eu.ejdr.domain.shared.version

/**
 * Version sémantique `MAJOR.MINOR.PATCH` comparable.
 *
 * Value object de domaine **transverse** : la comparaison de versions est une vraie
 * règle (déterminer si une release est plus récente que la version installée), elle
 * n'a donc pas sa place inline dans un use case. La placer ici la rend testable
 * isolément et réutilisable.
 *
 * Le parsing est **tolérant** par conception : les numéros de version proviennent de
 * sources externes (tags GitHub, `BuildConfig`) au format variable (`v1.2`, `1.2.3`).
 * Un préfixe `v` est ignoré, les composants manquants ou non numériques valent `0`.
 */
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) {

    /** Vrai si `this` représente une version strictement postérieure à [other]. */
    fun isNewerThan(other: SemanticVersion): Boolean {
        if (major != other.major) return major > other.major
        if (minor != other.minor) return minor > other.minor
        return patch > other.patch
    }

    companion object {
        /** Analyse une chaîne (`"v1.2.3"`, `"1.2"`, `"2"`…) en tolérant les formes partielles. */
        fun parse(raw: String): SemanticVersion {
            val parts = raw.removePrefix("v").split(".")
            fun component(index: Int) = parts.getOrNull(index)?.toIntOrNull() ?: 0
            return SemanticVersion(component(0), component(1), component(2))
        }
    }
}
