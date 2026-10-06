package eu.ejdr.presentation.features.session

import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.SessionLobby
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Statut d'une session dont le salon d'attente est encore ouvert (renvoyé par le serveur). */
private const val SESSION_STATUS_LOBBY = "LOBBY"

/** Statut d'une session dont la partie a démarré (renvoyé par le serveur). */
private const val SESSION_STATUS_ACTIVE = "ACTIVE"

/** Écran vers lequel mène un [SessionShortcut], selon l'avancement de la session. */
enum class SessionShortcutTarget {
    /** La session est encore au salon d'attente : le raccourci y ramène. */
    LOBBY,

    /** La partie a démarré : le raccourci mène directement à l'écran de jeu. */
    GAME,
}

/**
 * Raccourci vers la session en cours de l'utilisateur, salon **ou** partie.
 *
 * Porté par la bulle de retour globale : il contient exactement ce que
 * [eu.ejdr.presentation.navigation.Route.SessionLobby] ou
 * [eu.ejdr.presentation.navigation.Route.SessionGame] réclame pour naviguer, rien de plus.
 *
 * Il **change de cible** plutôt que de disparaître quand le MJ démarre : on peut avoir quitté le
 * salon sans avoir quitté la session, et il faut alors un chemin vers la partie — sinon le joueur
 * (et le MJ lui-même) se retrouve dehors sans retour possible.
 *
 * @property sessionId Identifiant de la session à rouvrir.
 * @property title Titre de la session (affiché dans l'accessibilité de la bulle et l'en-tête de l'écran).
 * @property target Écran visé, déduit du statut serveur de la session.
 */
data class SessionShortcut(
    val sessionId: String,
    val title: String,
    val target: SessionShortcutTarget,
)

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
     * Titre de la session ouverte, retenu à part : [updateLobby] ne reçoit que le lobby serveur,
     * qui ne le transporte pas, et le raccourci doit pourtant survivre à chaque rafraîchissement.
     */
    private var title: String = ""

    private val _shortcut = MutableStateFlow<SessionShortcut?>(null)

    /**
     * Raccourci vers la session en cours, ou `null` s'il n'y a rien à rejoindre. Non nul tant que
     * la session est au statut `LOBBY` (cible : le salon) **ou** `ACTIVE` (cible : la partie) ;
     * il ne retombe à `null` que quand l'état est vidé — retrait par le MJ, déconnexion — ou que
     * la session quitte ces deux statuts.
     *
     * L'état partagé survit à la sortie de l'écran du salon : c'est ce qui rend la bulle possible.
     * Revers assumé : il vit en mémoire, donc un redémarrage de l'application le perd.
     */
    val shortcut: StateFlow<SessionShortcut?> = _shortcut.asStateFlow()

    /**
     * Ouvre (ou remplace) le lobby courant. Appelé par le détail de session juste après un
     * `launch` réussi (MJ), ou par l'acceptation d'une invitation (joueur), avant de naviguer
     * vers l'écran de lobby.
     *
     * @param lobby Lobby renvoyé par le serveur (session en `LOBBY` + participants).
     * @param title Titre de la session, que le lobby serveur ne transporte pas et dont le
     * raccourci de retour a besoin pour naviguer.
     * @param members Membres du groupe (hors MJ), pour résoudre les pseudos et l'invitation.
     * @param canManage `true` pour le MJ (commandes visibles), `false` pour un joueur convié.
     */
    fun open(lobby: SessionLobby, title: String, members: List<GroupMember>, canManage: Boolean) {
        this.title = title
        _lobby.value = lobby
        _members.value = members
        _canManage.value = canManage
        refreshShortcut()
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
        refreshShortcut()
    }

    /**
     * Réinitialise le lobby : déconnexion, ou retrait du joueur par le MJ. Efface aussi le
     * raccourci, donc la bulle de retour disparaît dans la même recomposition.
     */
    fun clear() {
        title = ""
        _lobby.value = null
        _members.value = emptyList()
        _canManage.value = false
        refreshShortcut()
    }

    /**
     * Recalcule le raccourci depuis le lobby courant. Tenu à jour à la main plutôt que dérivé par
     * `combine(...).stateIn(scope)` : ça éviterait d'injecter une `CoroutineScope` dans un état
     * partagé qui n'en a aucun autre besoin.
     *
     * Tout statut autre que `LOBBY` et `ACTIVE` (session terminée, par exemple) efface le
     * raccourci : il n'y a plus d'écran où revenir.
     */
    private fun refreshShortcut() {
        val lobby = _lobby.value
        _shortcut.value =
            when (lobby?.status) {
                SESSION_STATUS_LOBBY ->
                    SessionShortcut(lobby.sessionId, title, SessionShortcutTarget.LOBBY)
                SESSION_STATUS_ACTIVE ->
                    SessionShortcut(lobby.sessionId, title, SessionShortcutTarget.GAME)
                else -> null
            }
    }
}
