package eu.ejdr.presentation.features.session.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Écran de jeu d'une session **active** — placeholder de première itération.
 *
 * Affiche un canvas de base (grille façon plateau de jeu) surmonté d'une carte de confirmation :
 * la partie a démarré. Le rôle est lu depuis [SessionLobbyState] (renseigné à l'entrée du lobby) :
 * le MJ voit une mention de ses futures commandes, les joueurs un message d'attente. Le vrai
 * plateau interactif et les commandes du MJ arriveront dans une prochaine étape.
 *
 * @param sessionId Identifiant de la session active (réservé pour le futur chargement d'état).
 * @param title Titre de la session (affiché sur le canvas).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun SessionGamePage(
    sessionId: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    val lobbyState = koinInject<SessionLobbyState>()
    val canManage by lobbyState.canManage.collectAsStateWithLifecycle()

    // Couleurs capturées hors du DrawScope (qui n'a pas accès au thème Compose).
    val gridColor = AppTheme.colors.hairline
    val boardColor = AppTheme.colors.beige

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Canvas de base : fond « plateau » + quadrillage régulier.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = boardColor)
            val step = 48f
            var x = step
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        // Carte de confirmation posée au centre du plateau.
        AppCard {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).padding(AppTheme.dimens.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
            ) {
                AppIcon(
                    imageVector = AppIcons.Castle,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    size = 48.dp,
                )
                AppText(text = "La partie a commencé !", style = AppTextStyle.Title)
                AppText(
                    text = title,
                    style = AppTextStyle.Subtitle,
                    color = AppTheme.colors.textSecondary,
                )
                AppText(
                    text = if (canManage) {
                        "Vous êtes le MJ. Vos commandes pour gérer la partie arriveront bientôt."
                    } else {
                        "Installez-vous : le MJ mène la partie. Le plateau interactif arrive bientôt."
                    },
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
