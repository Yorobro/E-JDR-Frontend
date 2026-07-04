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
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Boîte de dialogue de création d'une fiche (composant bête, réutilise [AppDialog]).
 *
 * Une fiche appartient à exactement une campagne : le sélecteur de campagne est obligatoire. Seules
 * les campagnes éligibles (où l'utilisateur n'est pas MJ) sont proposées ; quand la liste est vide,
 * un message d'aide invite à créer une campagne et la confirmation reste désactivée.
 *
 * @param campaigns Campagnes éligibles (déjà filtrées : l'utilisateur n'en est pas le MJ).
 * @param onDismiss Callback de fermeture sans création.
 * @param onConfirm Callback de confirmation, portant le nom saisi et la campagne choisie.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher sous les champs.
 */
@Composable
fun CreateCharacterSheetDialog(
    campaigns: List<Campaign>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, campaignId: String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }
    var selectedCampaignId by remember { mutableStateOf<String?>(null) }
    val fieldError = if (touched && name.isBlank()) "Le nom ne peut pas être vide" else null
    val selectedName = campaigns.firstOrNull { it.id == selectedCampaignId }?.name

    AppDialog(
        title = "Nouvelle fiche",
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { onConfirm(name.trim(), selectedCampaignId!!) },
        modifier = modifier,
        confirmEnabled = name.isNotBlank() && selectedCampaignId != null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            AppTextField(
                value = name,
                onValueChange = { name = it; touched = true },
                label = "Nom de la fiche",
                errorMessage = fieldError,
                modifier = Modifier.fillMaxWidth(),
            )
            if (campaigns.isEmpty()) {
                AppText(
                    text = "Aucune campagne disponible : crée d'abord une campagne " +
                        "(où tu n'es pas MJ pour y jouer).",
                    style = AppTextStyle.Caption,
                    color = AppTheme.colors.muted,
                )
            } else {
                AppDropdown(
                    value = selectedName,
                    options = campaigns.map { it.name },
                    onSelect = { chosen ->
                        selectedCampaignId = campaigns.firstOrNull { it.name == chosen }?.id
                    },
                    label = "Campagne",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FormError(message = errorMessage)
        }
    }
}
