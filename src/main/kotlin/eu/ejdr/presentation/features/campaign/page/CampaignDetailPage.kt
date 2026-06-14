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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.presentation.features.campaign.CampaignDetailViewModel
import eu.ejdr.presentation.features.campaign.component.LinkCharacterDialog
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page détail d'une campagne (composant INTELLIGENT).
 *
 * Affiche le nom de la campagne, la liste des fiches rattachées (avec détachement), et permet
 * de rattacher une de ses propres fiches via un dialog de sélection. Le ViewModel charge les
 * fiches rattachées et mes fiches.
 *
 * @param id Identifiant de la campagne.
 * @param name Nom de la campagne (affiché en titre).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CampaignDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        CampaignDetailViewModel(
            campaignId = id,
            listCampaignCharacters = get<ListCampaignCharactersUseCase>(),
            listLinkable = get<ListLinkableCharactersUseCase>(),
            linkCharacter = get<LinkCharacterToCampaignUseCase>(),
            unlinkCharacter = get<UnlinkCharacterFromCampaignUseCase>(),
        )
    }
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val mySheets by viewModel.linkableSheets.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showLink by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
        AppText(
            text = "Personnages",
            style = AppTextStyle.Subtitle,
            color = AppTheme.colors.textSecondary,
        )

        AppButton(
            label = "Rattacher une fiche",
            onClick = { showLink = true },
            leadingIcon = Icons.Filled.Add,
        )

        FormError(message = error)

        Box(modifier = Modifier.fillMaxSize()) {
            if (characters.isEmpty()) {
                AppText(
                    text = "Aucune fiche rattachée.",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = AppTheme.dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    items(characters, key = { it.id }) { sheet ->
                        CharacterSheetCard(
                            sheet = sheet,
                            onDelete = { viewModel.unlink(sheet.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showLink) {
        // Ne proposer que mes fiches qui ne sont pas déjà rattachées.
        val linkedIds = characters.map { it.id }.toSet()
        LinkCharacterDialog(
            sheets = mySheets.filterNot { it.id in linkedIds },
            onSelect = { sheetId ->
                showLink = false
                viewModel.link(sheetId)
            },
            onDismiss = { showLink = false },
        )
    }
}
