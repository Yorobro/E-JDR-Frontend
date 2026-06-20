package eu.ejdr.application.shared

import kotlin.coroutines.cancellation.CancellationException
import kotlin.Result as KotlinResult

/**
 * Variante de [runCatching] **sûre vis-à-vis des coroutines**.
 *
 * `runCatching` standard attrape *tout* [Throwable], y compris la
 * [CancellationException] émise lorsqu'une coroutine est annulée (ex. l'écran qui
 * a lancé l'appel est quitté). L'annulation serait alors silencieusement convertie
 * en valeur d'échec ordinaire, ce qui **casse la concurrence structurée** : le
 * parent croit l'enfant terminé normalement, et l'erreur se manifeste plus tard
 * sous la forme d'un faux échec (typiquement une « erreur réseau » fantôme).
 *
 * Ce helper **rethrow** la [CancellationException] pour que l'annulation se propage
 * correctement, et n'encapsule que les autres throwables dans un [Result] en échec.
 * Il s'utilise exactement comme [runCatching] :
 *
 * ```kotlin
 * runCatchingCancellable { client.post(url) }
 *     .getOrElse { Result.Failure(AuthError.Network) }
 * ```
 *
 * Renvoie un [kotlin.Result] — à ne pas confondre avec le [Result] maison du
 * domaine ([eu.ejdr.application.shared.Result]) ; les types sont pleinement
 * qualifiés ici pour lever l'ambiguïté de nom dans ce package.
 *
 * @param block Bloc potentiellement suspendu à exécuter.
 * @return Un [kotlin.Result] portant la valeur, ou l'échec pour tout throwable
 * **autre** que l'annulation.
 * @throws CancellationException toujours rethrow telle quelle.
 */
inline fun <T> runCatchingCancellable(block: () -> T): KotlinResult<T> =
    try {
        KotlinResult.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        KotlinResult.failure(throwable)
    }
