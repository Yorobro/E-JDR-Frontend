package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppDivider
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Organisme d'ossature des écrans de la zone connectée.
 *
 * Empile une barre supérieure ([topBar]) puis le contenu de l'écran, garantissant ainsi la
 * présence de la top bar « partout » dans la zone connectée. Le contenu occupe tout l'espace
 * restant. Composant bête : il ne fait que disposer les slots reçus.
 *
 * @param topBar Slot de la barre supérieure (typiquement [AppTopBar]).
 * @param modifier Modifier Compose appliqué à l'ossature.
 * @param content Slot du contenu de l'écran, affiché sous la barre.
 */
@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        topBar()
        AppDivider()
        Box(Modifier.weight(1f)) {
            content()
        }
    }
}
