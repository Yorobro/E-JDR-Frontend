package eu.ejdr.presentation.shared.util

/**
 * Repliage des diacritiques latins vers leur base ASCII.
 *
 * Un tri naïf compare les points de code : « Élan » (U+00C9) passerait alors **après** « Zèbre »,
 * ce qui n'est pas l'ordre attendu d'un catalogue français. `java.text.Collator` ferait le travail,
 * mais il est **JVM-only** et ne compile pas en `commonMain` — d'où ce repliage manuel.
 *
 * Les majuscules accentuées sont couvertes : [foldForSort] passe en minuscules avant de replier.
 */
private val ACCENT_FOLDING: Map<Char, String> = mapOf(
    'à' to "a", 'á' to "a", 'â' to "a", 'ã' to "a", 'ä' to "a", 'å' to "a",
    'ç' to "c",
    'è' to "e", 'é' to "e", 'ê' to "e", 'ë' to "e",
    'ì' to "i", 'í' to "i", 'î' to "i", 'ï' to "i",
    'ñ' to "n",
    'ò' to "o", 'ó' to "o", 'ô' to "o", 'õ' to "o", 'ö' to "o",
    'ù' to "u", 'ú' to "u", 'û' to "u", 'ü' to "u",
    'ý' to "y", 'ÿ' to "y",
    'æ' to "ae", 'œ' to "oe", 'ß' to "ss",
)

/**
 * Clé de tri d'un libellé : minuscules, diacritiques repliés. « Épée » → « epee ».
 *
 * @param value Le libellé à normaliser.
 * @return La clé de comparaison.
 */
fun foldForSort(value: String): String =
    value.lowercase().map { char -> ACCENT_FOLDING[char] ?: char.toString() }.joinToString("")

/**
 * Ordre alphabétique insensible à la casse **et** aux accents.
 *
 * Départage les libellés de même clé par la chaîne brute, afin de rester un ordre **total** et
 * **stable** : « Elfe » et « elfe » ne s'échangent pas d'un chargement à l'autre.
 */
val AlphabeticalOrder: Comparator<String> =
    compareBy<String> { foldForSort(it) }.thenBy { it }

/**
 * Trie une liste par le libellé extrait via [selector], selon [AlphabeticalOrder].
 *
 * @param selector Extrait le libellé d'un élément.
 * @return Une nouvelle liste triée (l'originale n'est pas modifiée).
 */
fun <T> List<T>.sortedAlphabeticallyBy(selector: (T) -> String): List<T> =
    sortedWith(compareBy(AlphabeticalOrder, selector))
