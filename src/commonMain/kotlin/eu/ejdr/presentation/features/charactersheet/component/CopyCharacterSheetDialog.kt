package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Boîte de dialogue de copie d'une fiche vers une autre campagne (composant bête, réutilise
 * [AppDialog]).
 *
 * La copie crée une nouvelle fiche PENDING dans la campagne cible. Seul un sélecteur de campagne
 * éligible est proposé (l'utilisateur n'en est pas le MJ) ; quand la liste est vide, un message
 * d'aide s'affiche et la confirmation reste désactivée.
 *
 * @param sheetName Nom de la fiche à copier (affiché en contexte).
 * @param campaigns Campagnes éligibles cibles (déjà filtrées : l'utilisateur n'en est pas le MJ).
 * @param onDismiss Callback de fermeture sans copie.
 * @param onConfirm Callback de confirmation, portant la campagne cible choisie.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher sous le champ.
 * @param excludeCampaignId Campagne actuelle de la fiche source, exclue du choix (on ne copie pas
 *   une fiche vers sa propre campagne).
 */
@Composable
fun CopyCharacterSheetDialog(
    sheetName: String,
    campaigns: List<Campaign>,
    onDismiss: () -> Unit,
    onConfirm: (campaignId: String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    excludeCampaignId: String? = null,
) {
    val targetCampaigns = campaigns.filter { it.id != excludeCampaignId }
    var selectedCampaignId by remember { mutableStateOf<String?>(null) }
    val selectedName = targetCampaigns.firstOrNull { it.id == selectedCampaignId }?.name

    AppDialog(
        title = "Copier « $sheetName »",
        onDismiss = onDismiss,
        confirmLabel = "Copier",
        onConfirm = { onConfirm(selectedCampaignId!!) },
        modifier = modifier,
        confirmEnabled = selectedCampaignId != null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            if (targetCampaigns.isEmpty()) {
                AppText(
                    text = "Aucune autre campagne disponible : crée d'abord une campagne " +
                        "(où tu n'es pas MJ pour y jouer).",
                    style = AppTextStyle.Caption,
                    color = AppTheme.colors.muted,
                )
            } else {
                AppDropdown(
                    value = selectedName,
                    options = targetCampaigns.map { it.name },
                    onSelect = { chosen ->
                        selectedCampaignId = targetCampaigns.firstOrNull { it.name == chosen }?.id
                    },
                    label = "Campagne cible",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FormError(message = errorMessage)
        }
    }
}
