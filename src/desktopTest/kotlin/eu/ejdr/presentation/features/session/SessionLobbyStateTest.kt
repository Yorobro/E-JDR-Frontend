package eu.ejdr.presentation.features.session

import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.SessionLobby
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de l'état partagé du salon d'attente, centrés sur le **raccourci de retour** : c'est lui
 * qui pilote l'affichage de la bulle globale, donc sa présence et sa cible doivent suivre
 * exactement le statut de la session — un bouton vers un écran qui n'existe plus serait un
 * cul-de-sac pour l'utilisateur.
 */
class SessionLobbyStateTest {

    private fun lobby(status: String) = SessionLobby(
        sessionId = "s-1",
        status = status,
        participants = listOf(LobbyParticipant(userId = "u-2", status = "INVITED", characterSheetId = null)),
    )

    private val members =
        listOf(GroupMember(userId = "u-2", pseudo = "Gwen", role = "MEMBER", createdAt = "2026-01-01"))

    @Test
    fun `aucun raccourci tant qu'aucun salon n'est ouvert`() {
        assertNull(SessionLobbyState().shortcut.value)
    }

    @Test
    fun `ouvrir un salon expose le raccourci avec le titre de la session`() {
        val state = SessionLobbyState()

        state.open(lobby("LOBBY"), title = "La Tour de Guet", members = members, canManage = true)

        assertEquals(
            SessionShortcut("s-1", "La Tour de Guet", SessionShortcutTarget.LOBBY),
            state.shortcut.value,
        )
    }

    @Test
    fun `le raccourci vise la partie quand elle demarre`() {
        val state = SessionLobbyState()
        state.open(lobby("LOBBY"), title = "La Tour de Guet", members = members, canManage = false)

        // Chemin réel : le salon (ou la veille globale) recharge sur `session-status` et republie.
        state.updateLobby(lobby("ACTIVE"))

        // Il ne disparaît pas : quitter le salon ne veut pas dire quitter la session, et sans ce
        // raccourci l'utilisateur resté dehors n'aurait plus aucun chemin vers l'écran de jeu.
        assertEquals(
            SessionShortcut("s-1", "La Tour de Guet", SessionShortcutTarget.GAME),
            state.shortcut.value,
        )
    }

    @Test
    fun `le raccourci disparait quand la session quitte salon et partie`() {
        val state = SessionLobbyState()
        state.open(lobby("LOBBY"), title = "La Tour de Guet", members = members, canManage = false)

        state.updateLobby(lobby("COMPLETED"))

        assertNull(state.shortcut.value, "une session terminée n'a plus d'écran où revenir")
    }

    @Test
    fun `un rafraichissement du salon conserve le raccourci et son titre`() {
        val state = SessionLobbyState()
        state.open(lobby("LOBBY"), title = "La Tour de Guet", members = members, canManage = false)

        // Le lobby renvoyé par le serveur ne transporte pas le titre : il doit survivre quand même.
        state.updateLobby(lobby("LOBBY"))

        assertEquals("La Tour de Guet", state.shortcut.value?.title)
    }

    @Test
    fun `vider l'etat efface le raccourci`() {
        val state = SessionLobbyState()
        state.open(lobby("LOBBY"), title = "La Tour de Guet", members = members, canManage = true)

        state.clear()

        assertNull(state.shortcut.value)
    }
}
