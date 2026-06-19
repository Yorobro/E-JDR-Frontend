package eu.ejdr.domain.features.friendgroup.error

import eu.ejdr.domain.shared.error.DomainError

sealed class FriendGroupError(override val message: String) : DomainError {
    data object NotFound : FriendGroupError("Groupe introuvable.")
    data object NotMember : FriendGroupError("Vous n'êtes pas membre de ce groupe.")
    data object NotAdmin : FriendGroupError("Action réservée à l'administrateur du groupe.")
    data object InvitationNotFound : FriendGroupError("Invitation introuvable.")
    data object InvitationAlreadyResolved : FriendGroupError("Cette invitation a déjà été traitée.")
    data object AlreadyMember : FriendGroupError("Cet utilisateur est déjà membre du groupe.")
    data object InvitedUserNotFound : FriendGroupError("Aucun utilisateur trouvé avec cet e-mail.")
    data object CannotRemoveLastAdmin : FriendGroupError("Impossible de retirer le dernier administrateur.")
    data object InvalidGroupName : FriendGroupError("Le nom du groupe est invalide (1–120 caractères).")
    data object Network : FriendGroupError("Erreur réseau, vérifiez votre connexion.")
    data class Unknown(val detail: String) : FriendGroupError("Une erreur inattendue s'est produite.")
}
