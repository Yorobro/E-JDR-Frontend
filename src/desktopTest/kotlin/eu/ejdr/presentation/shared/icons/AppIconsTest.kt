package eu.ejdr.presentation.shared.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {
    @Test
    fun `les 23 icones sont definies et non vides`() {
        val all = listOf(
            AppIcons.Add, AppIcons.AccountCircle, AppIcons.Badge, AppIcons.Castle,
            AppIcons.Category, AppIcons.Close, AppIcons.ContentCopy, AppIcons.Delete,
            AppIcons.Edit, AppIcons.Group, AppIcons.Groups, AppIcons.Home, AppIcons.Mail,
            AppIcons.MenuBook, AppIcons.Person, AppIcons.PersonOutline, AppIcons.Settings,
            AppIcons.Visibility, AppIcons.VisibilityOff, AppIcons.ArrowBack, AppIcons.List,
            AppIcons.Email, AppIcons.Lock,
        )
        assertEquals(23, all.size)
        all.forEach { assertTrue(it.defaultWidth.value > 0f) }
    }
}
