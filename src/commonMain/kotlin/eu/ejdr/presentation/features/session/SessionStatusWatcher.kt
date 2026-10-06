package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.shared.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Ressource poussée par le serveur quand une session change d'état (le MJ démarre la partie). */
private const val RESOURCE_SESSION_STATUS = "session-status"

/**
 * Veille **globale** sur l'avancement de la session où l'utilisateur est engagé.
 *
 * Vit à la racine de l'application, comme [SessionRemovalWatcher] et pour la même raison : le
 * démarrage de la partie doit être pris en compte **où que se trouve** l'utilisateur. Le
 * [SessionLobbyViewModel] ne peut pas s'en charger — il meurt avec la page du salon, et c'est
 * précisément l'utilisateur qui a quitté cette page qui a besoin qu'on tienne son raccourci à jour.
 *
 * Effet concret : à la seconde où le MJ démarre, le raccourci de [SessionLobbyState] bascule de
 * `LOBBY` à `GAME` et la bulle passe de « Retour au salon » à « Rejoindre la partie ».
 *
 * **On ne navigue pas ici.** L'utilisateur a quitté le salon délibérément ; le téléporter depuis
 * l'écran où il se trouve (une fiche de personnage à moitié remplie, par exemple) lui ferait perdre
 * sa saisie. On lui rend le chemin visible, il le prend quand il veut.
 *
 * Le signal arrive sur deux canaux — `group:{id}` pour les écrans du groupe, `user:{id}` pour le
 * canal personnel, auquel le serveur abonne chaque socket dès le handshake. Seul le second porte
 * jusqu'à un utilisateur parti ailleurs dans l'application ; on ne filtre pas le canal, les deux
 * déclenchent le même rechargement (idempotent).
 *
 * Si le signal se perd malgré tout (socket reconnectée, application en veille), rien n'est cassé :
 * le raccourci reste sur le salon, et [SessionLobbyViewModel] rattrape au clic — il recharge, voit
 * `ACTIVE` et bascule vers l'écran de jeu. La bulle est un raccourci, pas l'unique chemin.
 *
 * @property scope Portée de coroutine qui porte l'écoute (celle de la racine).
 * @property invalidationBus Bus d'invalidation temps réel.
 * @property lobbyState État partagé de la session, dont le raccourci est rafraîchi.
 * @property getSessionLobby Lecture REST du salon (statut + participants), autorisée à tout membre
 * du groupe quel que soit le statut de la session.
 */
class SessionStatusWatcher(
    scope: CoroutineScope,
    private val invalidationBus: InvalidationBus,
    private val lobbyState: SessionLobbyState,
    private val getSessionLobby: GetSessionLobbyUseCase,
) {

    init {
        scope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource != RESOURCE_SESSION_STATUS) return@collect
                // Sans raccourci, l'utilisateur n'est engagé dans aucune session : l'événement
                // concerne un groupe qu'il regarde, pas lui.
                val sessionId = lobbyState.shortcut.value?.sessionId ?: return@collect
                // Best-effort : en cas d'échec réseau on garde l'état courant, le rattrapage au
                // clic reste disponible.
                getSessionLobby(sessionId).onSuccess { lobbyState.updateLobby(it) }
            }
        }
    }
}
