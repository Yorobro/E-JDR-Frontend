package eu.ejdr.presentation.features.friendgroup.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import eu.ejdr.presentation.shared.component.organism.AppCard
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
    AppCard(
        modifier = modifier,
        onClick = onClick,
        selected = isActive,
        elevation = AppTheme.dimens.elevationSm,
    ) {
        Column {
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
 * Composant bête : affiche l'identité du membre et son rôle, et expose deux actions distinctes,
 * mutuellement exclusives, décidées par l'appelant :
 * - [canLeave] : c'est **ma** carte → bouton « Quitter le groupe » (départ volontaire, tout membre).
 * - [canRemove] : carte d'un **autre** membre que je peux gérer → bouton « Retirer » (admin uniquement).
 *
 * Le sélecteur de rôle ([canManageRole]) reste réservé aux admins. Les deux actions appellent le
 * même [onRemove] : la distinction quitter/retirer est portée par l'autorisation back (un membre ne
 * peut retirer que lui-même ; retirer autrui exige le rôle admin).
 *
 * @param member Membre affiché (rôle `"ADMIN"`, `"MJ"` ou `"MEMBER"`).
 * @param canLeave Affiche le bouton « Quitter le groupe » si vrai (réservé à la carte de l'utilisateur courant).
 * @param canRemove Affiche le bouton « Retirer » si vrai (admin agissant sur un autre membre).
 * @param canManageRole Affiche le sélecteur de rôle si vrai (admin uniquement).
 * @param onRemove Callback de retrait/départ du membre.
 * @param onChangeRole Callback de changement de rôle ; reçoit le nouveau rôle (`"ADMIN"`/`"MJ"`/`"MEMBER"`).
 */
@Composable
fun MemberCard(
    member: GroupMember,
    canLeave: Boolean,
    canRemove: Boolean,
    canManageRole: Boolean,
    onRemove: () -> Unit,
    onChangeRole: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        onClick = null,
        elevation = AppTheme.dimens.elevationSm,
        shape = RoundedCornerShape(AppTheme.dimens.radiusSm),
        containerColor = AppTheme.colors.beige,
        contentPadding = PaddingValues(AppTheme.dimens.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                if (canLeave) {
                    AppButton(
                        label = "Quitter le groupe",
                        onClick = onRemove,
                        variant = ButtonVariant.Danger,
                    )
                } else if (canRemove) {
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
    AppCard(modifier = modifier, onClick = null) {
        Column {
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
