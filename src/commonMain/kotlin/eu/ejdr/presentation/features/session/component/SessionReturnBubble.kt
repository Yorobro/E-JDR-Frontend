package eu.ejdr.presentation.features.session.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.features.session.SessionShortcut
import eu.ejdr.presentation.features.session.SessionShortcutTarget
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme

/** Forme pilule : un arrondi de 50 % suit la hauteur du contenu quel que soit le thème. */
private val BubbleShape = RoundedCornerShape(percent = 50)

/** Échelle de départ/arrivée de l'animation : la bulle éclot depuis son centre. */
private const val POP_SCALE = 0.8f

/**
 * Bulle flottante de retour à la session en cours, ancrée **en bas à droite**.
 *
 * On peut quitter la page du salon — puis celle de la partie — tout en restant engagé dans la
 * session côté serveur : cette bulle est le chemin de retour. Elle flotte **hors du flux** plutôt
 * que de se glisser à côté du titre : un essai précédent l'a montré, sur une largeur de téléphone
 * elle s'y disputait la place avec les boutons d'action de
 * [eu.ejdr.presentation.shared.component.organism.PageHeader] (« Nouvelle campagne »…). Une
 * version en bandeau centré en haut avait aussi été rejetée : elle masquait le contenu.
 *
 * Elle s'affiche tant que [SessionLobbyState.shortcut] est non nul et **suit sa cible** : « Retour
 * au salon » tant que la session attend au lobby, « Rejoindre la partie » dès que le MJ a démarré
 * (voir [eu.ejdr.presentation.features.session.SessionStatusWatcher]). Elle disparaît quand l'état
 * est vidé — joueur retiré par le MJ, déconnexion.
 *
 * **Où la monter** : sur desktop, à la racine. Sur Android, **dans la zone de contenu**
 * d'`AppNavDisplay`, au-dessus de l'`AppBottomBar` — ainsi le dégagement au-dessus de la barre
 * découle de la mise en page au lieu d'être une hauteur codée en dur. Dans les deux cas
 * **avant** [eu.ejdr.presentation.shared.feedback.UiMessageHost], dont les messages occupent le
 * même bord et doivent passer par-dessus.
 *
 * @param lobbyState État partagé de la session, source du raccourci.
 * @param alreadyInSession `true` quand l'utilisateur est déjà sur l'écran visé (salon ou partie) :
 * la bulle n'aurait alors rien à proposer. Passé en paramètre plutôt que décidé ici — la bulle
 * ignore tout de la navigation — et pris en compte dans la visibilité plutôt qu'en ne montant pas
 * le composable, pour que l'animation de sortie puisse se jouer.
 * @param onOpenSession Appelé au clic avec le raccourci à ouvrir.
 * @param modifier Modifier Compose appliqué au conteneur.
 */
@Composable
fun SessionReturnBubble(
    lobbyState: SessionLobbyState,
    alreadyInSession: Boolean,
    onOpenSession: (SessionShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortcut by lobbyState.shortcut.collectAsStateWithLifecycle()

    // Le raccourci retombe à `null` **avant** la fin de l'animation de sortie : on garde le
    // dernier connu, sinon la bulle se viderait de son texte en disparaissant.
    var lastKnown by remember { mutableStateOf<SessionShortcut?>(null) }
    LaunchedEffect(shortcut) { shortcut?.let { lastKnown = it } }

    val motion = AppTheme.motion
    val duration = motion.effectiveDuration(motion.durationMedium)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = shortcut != null && !alreadyInSession,
            enter = fadeIn(tween(duration)) + scaleIn(tween(duration), initialScale = POP_SCALE),
            exit = fadeOut(tween(duration)) + scaleOut(tween(duration), targetScale = POP_SCALE),
            modifier = Modifier.align(Alignment.BottomEnd).padding(AppTheme.dimens.lg),
        ) {
            lastKnown?.let { target ->
                val label = when (target.target) {
                    SessionShortcutTarget.LOBBY -> "Retour au salon"
                    SessionShortcutTarget.GAME -> "Rejoindre la partie"
                }
                val icon = when (target.target) {
                    SessionShortcutTarget.LOBBY -> AppIcons.Groups
                    SessionShortcutTarget.GAME -> AppIcons.Play
                }
                AppSurface(
                    shape = BubbleShape,
                    color = AppTheme.colors.primary,
                    contentColor = AppTheme.colors.onPrimary,
                    // Élévation forte : la bulle doit se lire comme posée au-dessus du contenu,
                    // pas comme un élément de la page qu'elle survole.
                    elevation = AppTheme.dimens.elevationLg,
                    onClick = { onOpenSession(target) },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = AppTheme.dimens.md,
                            vertical = AppTheme.dimens.sm,
                        ),
                    ) {
                        AppIcon(
                            imageVector = icon,
                            contentDescription = "$label : ${target.title}",
                        )
                        AppText(
                            text = label,
                            style = AppTextStyle.Label,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
