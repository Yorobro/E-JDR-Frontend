package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import eu.ejdr.presentation.features.session.SessionShortcut
import eu.ejdr.presentation.features.session.SessionShortcutTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests de [openSession], centrés sur la règle qui le justifie : retirer les entrées **salon et
 * partie** de la session avant d'empiler la destination. Sans elle, Navigation 3 verrait deux
 * entrées de même clé (la bottom bar Android empile sans vider), et un retour arrière pourrait
 * ramener dans un salon que la partie a déjà remplacé.
 */
class SessionShortcutNavigationTest {

    private fun stackOf(vararg routes: NavKey) = NavBackStack<NavKey>().apply { addAll(routes) }

    private fun shortcut(target: SessionShortcutTarget, sessionId: String = "s-1") =
        SessionShortcut(sessionId, "La Tour de Guet", target)

    @Test
    fun `empile le salon au sommet quand il n'est pas dans la pile`() {
        val stack = stackOf(Route.Home)

        stack.openSession(shortcut(SessionShortcutTarget.LOBBY))

        assertEquals(listOf(Route.Home, Route.SessionLobby("s-1", "La Tour de Guet")), stack.toList())
    }

    @Test
    fun `un salon qui dort dans la pile est remonté au sommet sans doublon`() {
        val stack = stackOf(Route.Home, Route.SessionLobby("s-1", "La Tour de Guet"), Route.Campaigns)

        stack.openSession(shortcut(SessionShortcutTarget.LOBBY))

        assertEquals(
            listOf(Route.Home, Route.Campaigns, Route.SessionLobby("s-1", "La Tour de Guet")),
            stack.toList(),
        )
    }

    @Test
    fun `rejoindre la partie retire aussi le salon de la session`() {
        val stack = stackOf(Route.Home, Route.SessionLobby("s-1", "La Tour de Guet"), Route.Campaigns)

        stack.openSession(shortcut(SessionShortcutTarget.GAME))

        assertEquals(
            listOf(Route.Home, Route.Campaigns, Route.SessionGame("s-1", "La Tour de Guet")),
            stack.toList(),
        )
    }

    @Test
    fun `une partie déjà dans la pile n'est pas dupliquée`() {
        val stack = stackOf(Route.Home, Route.SessionGame("s-1", "La Tour de Guet"), Route.Campaigns)

        stack.openSession(shortcut(SessionShortcutTarget.GAME))

        assertEquals(
            listOf(Route.Home, Route.Campaigns, Route.SessionGame("s-1", "La Tour de Guet")),
            stack.toList(),
        )
    }

    @Test
    fun `les entrées d'une autre session sont conservées`() {
        val other = Route.SessionLobby("s-2", "Autre table")
        val stack = stackOf(Route.Home, other)

        stack.openSession(shortcut(SessionShortcutTarget.LOBBY))

        assertEquals(listOf(Route.Home, other, Route.SessionLobby("s-1", "La Tour de Guet")), stack.toList())
    }
}
