package eu.ejdr.domain.features.charactersheet.entities

/**
 * Vue lecture seule d'une campagne à laquelle une fiche est rattachée (onglet Campagnes).
 *
 * @property campaignId Identifiant de la campagne.
 * @property campaignName Nom (titre) de la campagne.
 * @property gameMasterPseudo Pseudo du maître du jeu (sous-titre).
 * @property linkStatus Statut du rattachement : `"PENDING"` (en attente du MJ) ou `"ACCEPTED"`.
 */
data class SheetCampaign(
    val campaignId: String,
    val campaignName: String,
    val gameMasterPseudo: String,
    val linkStatus: String,
)
