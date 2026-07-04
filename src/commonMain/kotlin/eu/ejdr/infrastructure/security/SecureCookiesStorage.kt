package eu.ejdr.infrastructure.security

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.isSecure

/**
 * Décore un CookiesStorage en mémoire. Persiste UNIQUEMENT le refresh_token,
 * chiffré sur disque, pour permettre l'auto-login après redémarrage.
 * L'access_token reste en mémoire seulement.
 *
 * Le refresh_token chargé depuis le disque est ré-injecté paresseusement, mais
 * **uniquement pour les requêtes vers le host du backend** ([backendUrl]). C'est
 * crucial : au démarrage, un appel concurrent vers un autre host (vérification de
 * mise à jour sur GitHub) partage le même client donc le même storage ; sans ce
 * filtre, ce premier appel consommerait la ré-injection et figerait le cookie sur le
 * mauvais domaine (Ktor remplit `domain`/`path` manquants à partir de l'URL de
 * requête via `fillDefaults`), si bien que `POST /auth/refresh` partirait sans
 * refresh_token → 401 → session effacée → reconnexion forcée à chaque lancement.
 *
 * Le cookie ré-injecté est reconstruit avec les mêmes attributs que ceux posés par
 * le backend (`path=/`, `domain` = host du backend, `secure` selon le schéma) pour
 * que le matching domaine/chemin de Ktor le renvoie bien sur toutes les routes du
 * backend, et pas seulement `/auth/refresh`.
 */
class SecureCookiesStorage(
    private val sessionStorage: SessionStorage,
    backendUrl: String,
    private val delegate: CookiesStorage = AcceptAllCookiesStorage(),
) : CookiesStorage, SessionPersistence {

    private val refreshTokenName = "refresh_token"
    private val backend = Url(backendUrl)
    private var pendingRefreshToken: String? = sessionStorage.load()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        delegate.addCookie(requestUrl, cookie)
        if (cookie.name == refreshTokenName) {
            sessionStorage.save(cookie.value)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        // Ne ré-injecter que sur le host du backend : un appel concurrent vers un autre
        // host (ex. GitHub pour la mise à jour) ne doit pas consommer la ré-injection.
        if (pendingRefreshToken != null && requestUrl.host == backend.host) {
            val value = pendingRefreshToken
            pendingRefreshToken = null
            if (value != null) {
                delegate.addCookie(requestUrl, restoredCookie(value))
            }
        }
        return delegate.get(requestUrl)
    }

    override fun close() = delegate.close()

    override fun hasPersistedSession(): Boolean = sessionStorage.exists()

    override fun clearPersisted() = sessionStorage.clear()

    /**
     * Reconstruit le cookie refresh_token avec les attributs posés par le backend.
     *
     * On fixe explicitement `domain`, `path` et `secure` plutôt que de laisser Ktor les
     * déduire de l'URL de requête : sinon le cookie se figerait sur le chemin de la
     * première requête (ex. `/auth/refresh`) et ne serait plus renvoyé aux autres routes.
     */
    private fun restoredCookie(value: String): Cookie = Cookie(
        name = refreshTokenName,
        value = value,
        domain = backend.host,
        path = "/",
        secure = backend.protocol.isSecure(),
        httpOnly = true,
    )
}
