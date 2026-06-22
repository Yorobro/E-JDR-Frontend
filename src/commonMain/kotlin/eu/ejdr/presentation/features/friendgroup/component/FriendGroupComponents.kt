package eu.ejdr.presentation.features.friendgroup.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.modifier.interactiveCard
import eu.ejdr.presentation.shared.component.modifier.interactiveCardElevation
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

@Composable
fun GroupCard(
    group: FriendGroup,
    isActive: Boolean,
    onActivate: () -> Unit,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) AppTheme.colors.primary else AppTheme.colors.border
    val borderWidth = if (isActive) 2.dp else 1.dp
    val interactionSource = remember { MutableInteractionSource() }
    val elevation = interactiveCardElevation(interactionSource, enabled = true, base = AppTheme.dimens.elevationSm)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().interactiveCard(interactionSource, enabled = true),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(borderWidth, borderColor),
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    ) {
        Column(modifier = Modifier.padding(AppTheme.dimens.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(text = group.name, style = AppTextStyle.Subtitle)
                    Box(
                        modifier = Modifier
                            .padding(top = AppTheme.dimens.xs)
                            .clip(RoundedCornerShape(AppTheme.dimens.radiusSm))
                            .background(AppTheme.colors.beige)
                            .padding(horizontal = AppTheme.dimens.sm, vertical = AppTheme.dimens.xs),
                    ) {
                        AppText(
                            text = roleLabel(group.myRole),
                            style = AppTextStyle.Caption,
                            color = AppTheme.colors.text,
                        )
                    }
                    if (isActive) {
                        AppText(text = "● Groupe actif", style = AppTextStyle.Body, color = AppTheme.colors.primary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs)) {
                    if (!isActive) {
                        AppButton(label = "Activer", onClick = onActivate, variant = ButtonVariant.Ghost)
                    }
                    if (onDelete != null) {
                        AppButton(label = "Supprimer", onClick = onDelete, variant = ButtonVariant.Danger)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }
    val fieldError = if (touched && name.isBlank()) "Le nom ne peut pas être vide" else null

    AppDialog(
        title = "Nouveau groupe",
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { if (name.isNotBlank()) onConfirm(name.trim()) },
        confirmEnabled = name.isNotBlank(),
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it; touched = true },
            label = "Nom du groupe",
            errorMessage = fieldError,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }

    AppDialog(
        title = "Inviter un membre",
        onDismiss = onDismiss,
        confirmLabel = "Inviter",
        onConfirm = { if (email.isNotBlank()) onConfirm(email.trim()) },
        confirmEnabled = email.isNotBlank(),
    ) {
        AppTextField(
            value = email,
            onValueChange = { email = it },
            label = "Adresse e-mail",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Carte d'un membre de groupe.
 *
 * Composant bête : affiche l'identité du membre et son rôle, et expose les actions de gestion
 * uniquement quand l'utilisateur courant est administrateur ([canManageRole]) :
 * sélecteur 3 rôles (ADMIN / MJ / MEMBER). Le retrait reste régi par
 * [canRemove] (calculé par l'appelant, ex. ne pas pouvoir retirer le dernier membre).
 *
 * @param member Membre affiché (rôle `"ADMIN"`, `"MJ"` ou `"MEMBER"`).
 * @param canRemove Affiche le bouton « Retirer » si vrai.
 * @param canManageRole Affiche le sélecteur de rôle si vrai (admin uniquement).
 * @param onRemove Callback de retrait du membre.
 * @param onChangeRole Callback de changement de rôle ; reçoit le nouveau rôle (`"ADMIN"`/`"MJ"`/`"MEMBER"`).
 */
@Composable
fun MemberCard(
    member: GroupMember,
    canRemove: Boolean,
    canManageRole: Boolean,
    onRemove: () -> Unit,
    onChangeRole: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.beige),
        border = BorderStroke(1.dp, AppTheme.colors.border),
        shape = RoundedCornerShape(AppTheme.dimens.radiusSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.dimens.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = member.pseudo.ifBlank { member.userId },
                    style = AppTextStyle.Body,
                )
                AppText(
                    text = roleLabel(member.role),
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs)) {
                if (canManageRole) {
                    AppDropdown(
                        value = roleLabel(member.role),
                        options = listOf("Admin", "MJ", "Membre"),
                        onSelect = { label ->
                            val newRole = roleValue(label)
                            if (newRole != member.role) onChangeRole(newRole)
                        },
                        label = "Rôle",
                        modifier = Modifier.width(140.dp),
                    )
                }
                if (canRemove) {
                    AppButton(label = "Retirer", onClick = onRemove, variant = ButtonVariant.Danger)
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "ADMIN" -> "Admin"
    "MJ" -> "MJ"
    "MEMBER" -> "Membre"
    else -> role
}

private fun roleValue(label: String): String = when (label) {
    "Admin" -> "ADMIN"
    "MJ" -> "MJ"
    "Membre" -> "MEMBER"
    else -> label
}

@Composable
fun InvitationCard(
    invitation: GroupInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.border),
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    ) {
        Column(modifier = Modifier.padding(AppTheme.dimens.md)) {
            AppText(text = invitation.groupName, style = AppTextStyle.Subtitle)
            AppText(
                text = "Invité par ${invitation.invitedByPseudo.ifBlank { invitation.invitedBy }}",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppTheme.dimens.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
            ) {
                AppButton(
                    label = "Accepter",
                    onClick = onAccept,
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    label = "Refuser",
                    onClick = onDecline,
                    variant = ButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
