package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Organisme barre de navigation supérieure (top bar) de la zone connectée.
 *
 * Composant bête : affiche le titre à gauche (précédé d'un bouton retour si [onBack] est fourni)
 * et les actions à droite (icônes de navigation).
 *
 * @param title Titre affiché à gauche.
 * @param modifier Modifier Compose appliqué à la barre.
 * @param onCampaigns Callback pour ouvrir les campagnes ; si `null`, l'icône est masquée.
 * @param onCharacterSheets Callback pour ouvrir les fiches ; si `null`, l'icône est masquée.
 * @param onGroups Callback pour ouvrir les groupes ; si `null`, l'icône est masquée.
 * @param onReferences Callback pour ouvrir « Mes éléments » ; si `null`, l'icône est masquée.
 * @param onSettings Callback pour ouvrir les paramètres ; si `null`, l'icône est masquée.
 * @param onInvitations Callback pour ouvrir les invitations reçues ; si `null`, l'icône est masquée.
 * @param onBack Callback pour revenir en arrière ; si `null`, le bouton est masqué.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onCampaigns: (() -> Unit)? = null,
    onCharacterSheets: (() -> Unit)? = null,
    onReferences: (() -> Unit)? = null,
    onGroups: (() -> Unit)? = null,
    onInvitations: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .heightIn(min = 56.dp)
            .padding(horizontal = AppTheme.dimens.lg, vertical = AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                    )
                }
            }
            AppText(text = title, style = AppTextStyle.Title)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TopBarAction(onCampaigns, Icons.AutoMirrored.Filled.List, "Campagnes")
            TopBarAction(onCharacterSheets, Icons.Default.Person, "Mes fiches")
            TopBarAction(onReferences, Icons.Default.Category, "Mes éléments")
            TopBarAction(onGroups, Icons.Default.Group, "Mes groupes")
            TopBarAction(onInvitations, Icons.Default.MailOutline, "Invitations")
            TopBarAction(onSettings, Icons.Default.Settings, "Paramètres")
        }
    }
}

/**
 * Action-icône de la top bar : rendue seulement si [onClick] est fourni.
 *
 * Factorise le motif « bouton-icône optionnel » répété pour chaque destination.
 *
 * @param onClick Callback du clic ; si `null`, rien n'est affiché.
 * @param icon Icône du bouton.
 * @param contentDescription Description d'accessibilité.
 */
@Composable
private fun TopBarAction(
    onClick: (() -> Unit)?,
    icon: ImageVector,
    contentDescription: String,
) {
    if (onClick != null) {
        IconButton(onClick = onClick) {
            AppIcon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}
