package eu.ejdr.presentation.shared.feedback

/** Tonalité d'un message UI transitoire. */
enum class UiMessageTone { SUCCESS, ERROR }

/**
 * Message UI transitoire à présenter à l'utilisateur (snackbar).
 *
 * @property text Texte affiché (dans la voix de l'app).
 * @property tone Tonalité visuelle (succès / erreur).
 */
data class UiMessage(val text: String, val tone: UiMessageTone) {
    companion object {
        fun success(text: String) = UiMessage(text, UiMessageTone.SUCCESS)
        fun error(text: String) = UiMessage(text, UiMessageTone.ERROR)
    }
}
