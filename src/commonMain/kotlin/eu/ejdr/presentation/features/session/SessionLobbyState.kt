package eu.ejdr.presentation.features.session

import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.SessionLobby
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Statut d'invitation d'un participant tel que renvoyé par le serveur. */
const val LOBBY_STATUS_INVITED = "INVITED"

/**
 * État partagé du lobby de session courant.
 *
 * Singleton Koin (comme [eu.ejdr.presentation.features.friendgroup.ActiveGroupState]) : il sert
 * de **point de passage** entre l'écran de détail de session — qui ouvre le lobby via
 * `POST /sessions/{id}/launch` puis navigue — et l'écran de lobby, qui l'observe. Le back-stack
 * Navigation 3 ne transporte que des clés sérialisables légères ; l'état riche du lobby (liste
 * de participants + membres conviables) vit donc ici.
 *
 * C'est aussi le **point d'entrée prévu des mises à jour temps réel** : quand les invitations
 * passeront par WebSocket, les réponses des joueurs viendront muter [lobby] ici, et l'écran de
 * lobby se recomposera sans changement. En attendant, [invite] applique une mise à jour
 * optimiste locale.
 *
 * NE PAS en faire un ViewModel (pas de ViewModelStoreOwner à la racine) : sa durée de vie doit
 * survivre au passage détail → lobby.
 */
class SessionLobbyState(
    private val uiMessageBus: UiMessageBus,
) {
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

    /**
     * Convie un joueur supplémentaire au lobby (cas d'un refus « accidentel » ou d'un oubli).
     *
     * Mise à jour **optimiste locale** : le participant est ajouté en `INVITED` immédiatement pour
     * un retour visuel instantané. À terme, l'envoi réel passera par WebSocket et cette méthode
     * relaiera la réponse du serveur plutôt que de simuler l'état. Sans effet si le joueur figure
     * déjà dans le lobby.
     *
     * @param userId Identifiant du joueur à convier.
     */
    fun invite(userId: String) {
        val current = _lobby.value ?: return
        if (current.participants.any { it.userId == userId }) return
        _lobby.value = current.copy(
            participants = current.participants + LobbyParticipant(userId, LOBBY_STATUS_INVITED, characterSheetId = null),
        )
        uiMessageBus.emit(UiMessage.success("Invitation envoyée"))
    }

    /**
     * Démarre réellement la session (transition `LOBBY` → `ACTIVE`), une fois tous les joueurs
     * présents. Le démarrage effectif (endpoint dédié + écran de jeu avec canvas) arrivera dans
     * une prochaine étape ; pour l'instant on informe l'utilisateur.
     */
    fun startSession() {
        uiMessageBus.emit(UiMessage.success("Le démarrage de la session arrivera dans une prochaine étape."))
    }

    /** Réinitialise le lobby (retour arrière hors du lobby, déconnexion). */
    fun clear() {
        _lobby.value = null
        _members.value = emptyList()
        _canManage.value = false
    }
}
