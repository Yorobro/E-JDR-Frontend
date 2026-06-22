package eu.ejdr.presentation.shared.util

private val MOIS = arrayOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** Parse les 10 premiers caractères ISO (yyyy-MM-dd) → (année, mois 1-12, jour), ou null. */
private fun parseIsoDate(iso: String): Triple<Int, Int, Int>? {
    val head = iso.take(10)
    val parts = head.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return Triple(y, m, d)
}

/** Nombre de jours depuis une époque arbitraire (algorithme du jour julien simplifié). */
private fun toEpochDay(y: Int, m: Int, d: Int): Long {
    var yy = y.toLong()
    var mm = m.toLong()
    if (mm <= 2) { yy -= 1; mm += 12 }
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val doy = (153 * (mm - 3) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** « 22 juin 2026 ». Entrée non parsable → renvoyée brute (jamais d'exception). */
fun formatDate(iso: String): String {
    val (y, m, d) = parseIsoDate(iso) ?: return iso
    return "$d ${MOIS[m - 1]} $y"
}

/** Indice relatif court, ou null hors fenêtre ±7 jours / entrée invalide. `todayIso` injecté. */
fun relativeDate(iso: String, todayIso: String): String? {
    val target = parseIsoDate(iso) ?: return null
    val today = parseIsoDate(todayIso) ?: return null
    val delta = toEpochDay(target.first, target.second, target.third) -
        toEpochDay(today.first, today.second, today.third)
    return when (delta) {
        0L -> "aujourd'hui"
        1L -> "demain"
        -1L -> "hier"
        in 2L..7L -> "dans $delta jours"
        in -7L..-2L -> "il y a ${-delta} jours"
        else -> null
    }
}
