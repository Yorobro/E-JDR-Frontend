package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import eu.ejdr.presentation.features.session.SessionShortcut
import eu.ejdr.presentation.features.session.SessionShortcutTarget

/**
 * Ouvre l'écran visé par un raccourci de session (salon ou partie) en le portant **au sommet** de
 * la pile.
 *
 * Partagé par les deux racines (desktop et Android) parce que la règle qu'il porte est subtile et
 * n'a aucune raison d'être réapprise deux fois : l'écran visé peut **déjà dormir dans la pile** —
 * la bottom bar Android empile sans vider, donc on peut avoir quitté le salon sans le dépiler, et
 * une partie quittée laisse la même trace. Or Navigation 3 identifie ses entrées **par leur clé** :
 * rempiler la même ferait cohabiter deux clés identiques. On retire donc les entrées existantes de
 * cette session — salon **et** partie — avant d'empiler la destination voulue. Retirer les deux
 * évite aussi qu'un retour arrière ne ramène dans un salon que la partie a déjà remplacé.
 *
 * @param shortcut Raccourci à ouvrir : identifiant, titre et écran visé.
 */
fun NavBackStack<NavKey>.openSession(shortcut: SessionShortcut) {
    removeAll { entry ->
        when (entry) {
            is Route.SessionLobby -> entry.id == shortcut.sessionId
            is Route.SessionGame -> entry.id == shortcut.sessionId
            else -> false
        }
    }
    add(
        when (shortcut.target) {
            SessionShortcutTarget.LOBBY -> Route.SessionLobby(shortcut.sessionId, shortcut.title)
            SessionShortcutTarget.GAME -> Route.SessionGame(shortcut.sessionId, shortcut.title)
        },
    )
}
