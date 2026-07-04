package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import eu.ejdr.application.shared.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Implémentation de [RealtimeConnection] portant la **machine à états de reconnexion**.
 *
 * La mécanique du socket est déléguée à un [RealtimeTransport] injecté ; cette classe
 * ne s'occupe que du cycle de vie : (re)connexion avec backoff ([ReconnectPolicy]),
 * exposition de l'[state], et republication des messages entrants vers [incoming].
 *
 * Possède **son propre** [CoroutineScope] (passé au constructeur) — non lié à un écran :
 * la connexion vit tant qu'elle est voulue. À la coupure (le flux du transport se
 * termine sur exception), elle repasse en [ConnectionState.Reconnecting] et retente
 * selon la politique ; après reconnexion, [onReconnected] est invoqué (hook de
 * re-souscription pour la future feature).
 *
 * @property scope Portée de coroutine propre à la connexion.
 * @property transport Transport bas niveau (ouverture/émission de trames).
 * @property reconnectPolicy Politique de backoff entre tentatives.
 * @property onReconnected Hook appelé après chaque reconnexion réussie (re-souscription).
 */
class KtorRealtimeConnection(
    private val scope: CoroutineScope,
    private val transport: RealtimeTransport,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    private val onReconnected: suspend () -> Unit = {},
) : RealtimeConnection {

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<RealtimeMessage>(extraBufferCapacity = 64)
    override val incoming: Flow<RealtimeMessage> = _incoming

    private var loop: Job? = null

    override suspend fun connect() {
        if (loop?.isActive == true) return
        loop = scope.launch { runConnectionLoop() }
    }

    override suspend fun send(message: RealtimeMessage) {
        transport.send(message)
    }

    override suspend fun sendRaw(text: String) {
        transport.sendRaw(text)
    }

    override suspend fun disconnect() {
        loop?.cancel()
        loop = null
        _state.value = ConnectionState.Disconnected
    }

    /**
     * Boucle de (re)connexion : ouvre une session, republie ses messages, et retente
     * avec backoff tant que la portée est active. Une terminaison **normale** du flux
     * (serveur ferme proprement) arrête la boucle ; une exception déclenche une
     * reconnexion.
     */
    private suspend fun runConnectionLoop() {
        var attempt = 0
        var firstConnect = true
        while (scope.isActive) {
            _state.value = if (firstConnect) ConnectionState.Connecting else ConnectionState.Reconnecting

            var crashed = false
            runCatchingCancellable {
                transport.open()
                    .catch { crashed = true }      // coupure : provoque une reconnexion
                    .collect { message ->
                        if (_state.value != ConnectionState.Connected) {
                            _state.value = ConnectionState.Connected
                            if (!firstConnect) onReconnected()
                            attempt = 0
                        }
                        firstConnect = false
                        _incoming.emit(message)
                    }
            }.onFailure { crashed = true }

            if (!crashed) {
                // Fin normale : le serveur a fermé proprement, on ne reconnecte pas.
                break
            }
            // Coupure : on attend selon le backoff avant de retenter.
            firstConnect = false
            delay(reconnectPolicy.delayForAttempt(attempt))
            attempt++
        }
        _state.value = ConnectionState.Disconnected
    }
}
