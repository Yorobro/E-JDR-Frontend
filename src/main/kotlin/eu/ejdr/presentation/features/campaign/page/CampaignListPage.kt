package eu.ejdr.presentation.features.campaign.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.features.campaign.CampaignListViewModel
import eu.ejdr.presentation.features.campaign.component.CampaignCard
import eu.ejdr.presentation.features.campaign.component.ConfirmDeleteDialog
import eu.ejdr.presentation.features.campaign.component.CreateCampaignDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page liste des campagnes (composant INTELLIGENT).
 *
 * Crée un [CampaignListViewModel] retenu par la destination et observe son état. Affiche une
 * carte par campagne, un bouton de création et gère localement l'état d'ouverture des deux
 * modals (création, confirmation de suppression). Le rendu est délégué à des composants bêtes.
 *
 * @param onOpenCampaign Callback d'ouverture du détail d'une campagne (id + nom).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CampaignListPage(
    onOpenCampaign: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        CampaignListViewModel(
            get<ListCampaignsUseCase>(),
            get<CreateCampaignUseCase>(),
            get<DeleteCampaignUseCase>(),
        )
    }
    val campaigns by viewModel.campaigns.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Campaign?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppButton(
            label = "Ajouter une campagne",
            onClick = { showCreate = true },
            leadingIcon = Icons.Filled.Add,
        )

        FormError(message = error)

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && campaigns.isEmpty() ->
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.primary,
                    )

                campaigns.isEmpty() ->
                    AppText(
                        text = "Aucune campagne pour le moment.",
                        style = AppTextStyle.Body,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = AppTheme.dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    items(campaigns, key = { it.id }) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            onClick = { onOpenCampaign(campaign.id, campaign.name) },
                            onDelete = { pendingDelete = campaign },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateCampaignDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.create(name)
            },
        )
    }

    pendingDelete?.let { campaign ->
        ConfirmDeleteDialog(
            campaignName = campaign.name,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(campaign.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
