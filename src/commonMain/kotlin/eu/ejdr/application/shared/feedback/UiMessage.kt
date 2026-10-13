package eu.ejdr.application.shared.feedback

/** Tonalité d'un message UI transitoire. */
enum class UiMessageTone { SUCCESS, ERROR }

/**
 * Bord de l'écran où présenter un message transitoire.
 *
 * [BOTTOM] est le défaut historique (retour d'une action que l'utilisateur vient de déclencher,
 * près de son geste) ; [TOP] est réservé aux messages **subis**, qui arrivent sans action de sa
 * part et doivent l'interrompre — typiquement une notification poussée par le serveur.
 */
enum class UiMessagePlacement { TOP, BOTTOM }

/**
 * Message UI transitoire à présenter à l'utilisateur (snackbar).
 *
 * Vit dans la couche application : c'est une donnée pure (aucune dépendance Compose) publiée par
 * les ViewModels sur le [UiMessageBus] et rendue par la présentation.
 *
 * @property text Texte affiché (dans la voix de l'app).
 * @property tone Tonalité visuelle (succès / erreur).
 * @property placement Bord d'affichage (bas par défaut, cf. [UiMessagePlacement]).
 */
data class UiMessage(
    val text: String,
    val tone: UiMessageTone,
    val placement: UiMessagePlacement = UiMessagePlacement.BOTTOM,
) {
    companion object {
        fun success(text: String, placement: UiMessagePlacement = UiMessagePlacement.BOTTOM) =
            UiMessage(text, UiMessageTone.SUCCESS, placement)

        fun error(text: String, placement: UiMessagePlacement = UiMessagePlacement.BOTTOM) =
            UiMessage(text, UiMessageTone.ERROR, placement)
    }
}
