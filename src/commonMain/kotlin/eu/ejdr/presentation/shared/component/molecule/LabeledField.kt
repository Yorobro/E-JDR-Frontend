package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Molécule générique « libellé + champ + erreur ».
 *
 * Composant réutilisable et sans dépendance métier : il affiche un libellé au-dessus d'un
 * contenu quelconque (fourni via le slot [content], typiquement un champ de saisie), suivi
 * d'un éventuel message d'erreur. Le slot rend la molécule indépendante du type de champ
 * (texte, mot de passe, nombre, etc.).
 *
 * @param label Libellé affiché au-dessus du contenu.
 * @param modifier Modifier Compose appliqué à la colonne.
 * @param errorMessage Message d'erreur affiché sous le contenu, ou `null` pour ne rien afficher.
 * @param content Contenu du champ (slot), affiché pleine largeur.
 */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        AppText(text = label, style = AppTextStyle.Label)
        content()
        FormError(errorMessage)
    }
}
