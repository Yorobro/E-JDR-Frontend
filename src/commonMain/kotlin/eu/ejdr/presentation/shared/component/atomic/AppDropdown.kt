package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.base.AppDropdownCore
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.component.molecule.LabeledField
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome menu déroulant du design system (choix parmi une liste fermée).
 *
 * Composant bête : affiche la valeur courante et propose [options] au clic. Réutilisable
 * (ex. sexe M/F/NB). La valeur peut être `null` (rien de sélectionné). Style « outlined »
 * cohérent avec [AppTextField] (ancre cliquable en lecture seule, couleurs du thème).
 *
 * @param value Valeur sélectionnée, ou `null`.
 * @param options Options proposées.
 * @param onSelect Callback de sélection.
 * @param label Libellé du champ.
 * @param modifier Modifier Compose appliqué au champ.
 */
@Composable
fun AppDropdown(
    value: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    var expanded by remember { mutableStateOf(false) }

    LabeledField(label = label, modifier = modifier) {
        AppDropdownCore(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchor = { anchorModifier ->
                AppSurface(
                    modifier = anchorModifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(dimens.radiusMd),
                    color = colors.surface,
                    contentColor = colors.text,
                    border = BorderStroke(
                        width = if (expanded) dimens.borderWidthFocused else dimens.borderWidth,
                        color = if (expanded) colors.primary else colors.border,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.md, vertical = dimens.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = value ?: label,
                            color = if (value != null) colors.text else colors.muted,
                            modifier = Modifier.weight(1f),
                        )
                        AppIcon(
                            imageVector = AppIcons.List,
                            contentDescription = null,
                            tint = colors.muted,
                        )
                    }
                }
            },
            content = {
                AppSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.radiusMd),
                    color = colors.surface,
                    contentColor = colors.text,
                    border = BorderStroke(dimens.borderWidth, colors.border),
                    elevation = dimens.elevationMd,
                ) {
                    Column {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(option)
                                        expanded = false
                                    }
                                    .padding(horizontal = dimens.md, vertical = dimens.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppText(text = option)
                            }
                        }
                    }
                }
            },
        )
    }
}
