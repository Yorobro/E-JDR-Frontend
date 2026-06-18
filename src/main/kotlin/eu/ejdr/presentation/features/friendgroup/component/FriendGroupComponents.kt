package eu.ejdr.presentation.features.friendgroup.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
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

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
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
                    AppText(
                        text = if (group.myRole == "ADMIN") "Admin" else "Membre",
                        style = AppTextStyle.Body,
                        color = AppTheme.colors.textSecondary,
                    )
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

    AppDialog(
        title = "Nouveau groupe",
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { if (name.isNotBlank()) onConfirm(name.trim()) },
        confirmEnabled = name.isNotBlank(),
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nom du groupe",
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

@Composable
fun MemberCard(
    member: GroupMember,
    canRemove: Boolean,
    onRemove: () -> Unit,
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
            Column {
                AppText(
                    text = member.userId.take(12) + if (member.userId.length > 12) "…" else "",
                    style = AppTextStyle.Body,
                )
                AppText(
                    text = if (member.role == "ADMIN") "Admin" else "Membre",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.textSecondary,
                )
            }
            if (canRemove) {
                AppButton(label = "Retirer", onClick = onRemove, variant = ButtonVariant.Danger)
            }
        }
    }
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
                text = "Invité par ${invitation.invitedBy}",
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
