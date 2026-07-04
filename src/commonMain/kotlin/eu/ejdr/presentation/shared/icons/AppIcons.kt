package eu.ejdr.presentation.shared.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Castle
import com.composables.icons.lucide.CircleUser
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.House
import com.composables.icons.lucide.IdCard
import com.composables.icons.lucide.List as LucideList
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.X

/**
 * Jeu d'icônes du design system, adossé à **Lucide** (`com.composables:icons-lucide-cmp`).
 *
 * L'API publique (les noms `AppIcons.Xxx`) est volontairement stable : le reste de l'app
 * référence ces noms via `AppIcon`, indépendamment de la source réelle des glyphes. Chaque
 * entrée pointe vers l'icône Lucide équivalente (couleur appliquée par `AppIcon` via le `tint`).
 * Certaines entrées sont des alias sémantiques d'une même icône (ex. `Groups` = `Group`).
 */
object AppIcons {

    val Add: ImageVector = Lucide.Plus
    val Close: ImageVector = Lucide.X
    val ArrowBack: ImageVector = Lucide.ArrowLeft
    val List: ImageVector = Lucide.LucideList
    val Delete: ImageVector = Lucide.Trash2
    val Edit: ImageVector = Lucide.Pencil
    val ContentCopy: ImageVector = Lucide.Copy
    val Person: ImageVector = Lucide.User
    val PersonOutline: ImageVector = Lucide.User
    val AccountCircle: ImageVector = Lucide.CircleUser
    val Group: ImageVector = Lucide.Users
    val Groups: ImageVector = Lucide.Users
    val Home: ImageVector = Lucide.House
    val Settings: ImageVector = Lucide.Settings
    val Category: ImageVector = Lucide.Shapes
    val Castle: ImageVector = Lucide.Castle
    val MenuBook: ImageVector = Lucide.BookOpen
    val Badge: ImageVector = Lucide.IdCard
    val Mail: ImageVector = Lucide.Mail
    val Email: ImageVector = Lucide.Mail
    val Lock: ImageVector = Lucide.Lock
    val Visibility: ImageVector = Lucide.Eye
    val VisibilityOff: ImageVector = Lucide.EyeOff
}
