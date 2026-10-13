package eu.ejdr.domain.features.session.entities

/**
 * Invitation de session en attente, du point de vue du joueur convié.
 *
 * Conteneur de données pur (domaine front anémique) : reflète la réponse de
 * `GET /sessions/invitations`. Porte de quoi afficher l'invitation **et** enclencher
 * l'acceptation : [groupId] permet d'activer le bon groupe de travail avant de rejoindre le lobby.
 *
 * @property sessionId Identifiant de la session concernée.
 * @property title Titre de la session.
 * @property date Date de la session au format `YYYY-MM-DD`.
 * @property campaignId Identifiant de la campagne parente.
 * @property campaignName Nom de la campagne parente (affichage).
 * @property groupId Identifiant du groupe de la campagne (à activer à l'acceptation).
 */
data class SessionInvitation(
    val sessionId: String,
    val title: String,
    val date: String,
    val campaignId: String,
    val campaignName: String,
    val groupId: String,
)
