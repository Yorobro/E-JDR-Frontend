package eu.ejdr.domain.features.session.entities

/**
 * Lobby d'une session : résultat de l'ouverture d'une session par le MJ.
 *
 * Conteneur de données pur (domaine front anémique) : reflète la réponse du serveur après
 * `POST /sessions/{id}/launch`. Le MJ n'apparaît pas dans [participants] (il accède à la session
 * via son rôle, sans invitation) ; seuls les joueurs conviés y figurent.
 *
 * @property sessionId Identifiant de la session passée en statut `LOBBY`.
 * @property status Statut de la session après l'ouverture du lobby (normalement `LOBBY`).
 * @property participants Joueurs conviés et leur état d'invitation.
 */
data class SessionLobby(
    val sessionId: String,
    val status: String,
    val participants: List<LobbyParticipant>,
)

/**
 * Participant convié à un lobby de session.
 *
 * @property userId Identifiant de l'utilisateur convié.
 * @property status État de l'invitation (`INVITED`, `ACCEPTED`, `REFUSED`).
 * @property characterSheetId Fiche de personnage choisie à l'acceptation, ou `null` tant qu'il n'a pas répondu.
 */
data class LobbyParticipant(
    val userId: String,
    val status: String,
    val characterSheetId: String?,
)
