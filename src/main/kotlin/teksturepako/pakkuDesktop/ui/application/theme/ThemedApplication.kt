package teksturepako.pakkuDesktop.ui.application.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.application
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.styling.TitleBarStyle
import teksturepako.pakkuDesktop.ui.viewmodel.ProfileViewModel

fun themedApplication(
    content: @Composable ApplicationScope.() -> Unit
) = application {
    val profileData by ProfileViewModel.profileData.collectAsState()

    val textStyle = JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default)

    val themeDefinition = if (profileData.intUiTheme.isDark())
    {
        JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
    }
    else
    {
        JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
    }

    IntUiTheme(
        themeDefinition, ComponentStyling.default().decoratedWindow(
            titleBarStyle = when (profileData.intUiTheme)
            {
                IntUiThemes.Light  -> TitleBarStyle.light()
                IntUiThemes.Dark   -> TitleBarStyle.dark()
                IntUiThemes.System -> if (profileData.intUiTheme.isDark())
                {
                    TitleBarStyle.dark()
                }
                else
                {
                    TitleBarStyle.light()
                }
            },
        ),
        swingCompatMode = false
    ) {
        content(this)
    }
}