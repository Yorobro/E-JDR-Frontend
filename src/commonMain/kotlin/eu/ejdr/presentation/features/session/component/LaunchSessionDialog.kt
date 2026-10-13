package eu.ejdr.presentation.features.session.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppCheckbox
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Boîte de dialogue d'ouverture du lobby (composant bête).
 *
 * Affiche la liste des joueurs sélectionnables ([members] = membres du groupe **hors MJ**) sous
 * forme de cases à cocher, avec un raccourci « tout cocher / tout décocher ». Le MJ n'apparaît
 * pas : il accède à la session via son rôle, sans s'inviter. La confirmation reste désactivée
 * tant qu'aucun joueur n'est coché (le serveur exige au moins un convié).
 *
 * @param members Joueurs sélectionnables (membres du groupe actif, hors MJ courant).
 * @param loading Désactive les actions pendant l'envoi de la requête.
 * @param onDismiss Callback de fermeture sans ouverture du lobby.
 * @param onConfirm Callback de confirmation, portant les identifiants des joueurs cochés.
 * @param modifier Modifier Compose appliqué au dialog.
 */
@Composable
fun LaunchSessionDialog(
    members: List<GroupMember>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (selectedUserIds: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val allSelected = members.isNotEmpty() && selected.size == members.size

    AppDialog(
        title = "Lancer la session",
        onDismiss = onDismiss,
        confirmLabel = "Lancer",
        onConfirm = { onConfirm(selected.toList()) },
        modifier = modifier,
        confirmEnabled = selected.isNotEmpty() && !loading,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            if (members.isEmpty()) {
                AppText(
                    text = "Aucun autre membre dans le groupe à convier.",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.textSecondary,
                )
            } else {
                AppText(
                    text = "Choisissez les joueurs qui participent à la session.",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.textSecondary,
                )
                AppButton(
                    label = if (allSelected) "Tout décocher" else "Tout cocher",
                    onClick = {
                        selected = if (allSelected) emptySet() else members.map { it.userId }.toSet()
                    },
                    variant = ButtonVariant.Ghost,
                    enabled = !loading,
                )
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
                ) {
                    members.forEach { member ->
                        AppCheckbox(
                            checked = member.userId in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + member.userId else selected - member.userId
                            },
                            label = member.pseudo.ifBlank { member.userId },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
