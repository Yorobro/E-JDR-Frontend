package eu.ejdr.presentation.features.session

import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.SessionLobby
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État partagé du lobby de session courant.
 *
 * Singleton Koin (comme [eu.ejdr.presentation.features.friendgroup.ActiveGroupState]) : il sert
 * de **point de passage** entre l'écran de détail de session — qui ouvre le lobby via
 * `POST /sessions/{id}/launch` puis navigue — et l'écran de lobby, qui l'observe. Le back-stack
 * Navigation 3 ne transporte que des clés sérialisables légères ; l'état riche du lobby (liste
 * de participants + membres conviables) vit donc ici.
 *
 * C'est aussi le **point d'entrée des mises à jour temps réel** : les réponses des joueurs et
 * les invitations envoyées depuis le salon rechargent le lobby serveur puis le publient ici via
 * [updateLobby], et l'écran de lobby se recompose sans changement.
 *
 * NE PAS en faire un ViewModel (pas de ViewModelStoreOwner à la racine) : sa durée de vie doit
 * survivre au passage détail → lobby.
 */
class SessionLobbyState {
    private val _lobby = MutableStateFlow<SessionLobby?>(null)

    /** Lobby courant, ou `null` si aucun lobby n'est ouvert (état initial / après [clear]). */
    val lobby: StateFlow<SessionLobby?> = _lobby.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())

    /**
     * Membres du groupe conviables (tous sauf le MJ) : sert à résoudre les pseudos des
     * participants et à alimenter le sélecteur d'invitation.
     */
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _canManage = MutableStateFlow(false)

    /**
     * `true` si l'occupant courant du lobby est le **MJ** (peut convier d'autres joueurs et
     * démarrer la session), `false` pour un **joueur** convié (vue en lecture seule). Pilote
     * l'affichage des commandes dans l'écran de lobby.
     */
    val canManage: StateFlow<Boolean> = _canManage.asStateFlow()

    /**
     * Ouvre (ou remplace) le lobby courant. Appelé par le détail de session juste après un
     * `launch` réussi (MJ), ou par l'acceptation d'une invitation (joueur), avant de naviguer
     * vers l'écran de lobby.
     *
     * @param lobby Lobby renvoyé par le serveur (session en `LOBBY` + participants).
     * @param members Membres du groupe (hors MJ), pour résoudre les pseudos et l'invitation.
     * @param canManage `true` pour le MJ (commandes visibles), `false` pour un joueur convié.
     */
    fun open(lobby: SessionLobby, members: List<GroupMember>, canManage: Boolean) {
        _lobby.value = lobby
        _members.value = members
        _canManage.value = canManage
    }

    /**
     * Rafraîchit **uniquement** le lobby courant (statut + participants) suite à une mise à jour
     * temps réel (une réponse de joueur), en conservant les membres et le rôle déjà en place.
     * Sans effet si aucun lobby n'est ouvert.
     *
     * @param lobby Lobby rechargé depuis le serveur.
     */
    fun updateLobby(lobby: SessionLobby) {
        if (_lobby.value == null) return
        _lobby.value = lobby
    }

    /** Réinitialise le lobby (retour arrière hors du lobby, déconnexion). */
    fun clear() {
        _lobby.value = null
        _members.value = emptyList()
        _canManage.value = false
    }
}
