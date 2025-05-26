package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel

@Composable
fun ThemeButton(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onClick: (currentTheme: IntUiThemes) -> Unit = { currentTheme ->
        if (currentTheme.isDark())
        {
            coroutineScope.launch {
                ProfileViewModel.updateTheme(IntUiThemes.Light)
            }
        }
        else
        {
            coroutineScope.launch {
                ProfileViewModel.updateTheme(IntUiThemes.Dark)
            }
        }
    }
)
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    if (profileData.intUiTheme.isDark())
    {
        IconButton(
            onClick = {
                onClick(profileData.intUiTheme)
            },
            Modifier.size(30.dp),
        ) {
            Icon(PakkuDesktopIcons.darkTheme, "dark_theme")
        }
    }
    else
    {
        IconButton(
            onClick = {
                onClick(profileData.intUiTheme)
            },
            Modifier.size(30.dp),
        ) {
            Icon(PakkuDesktopIcons.lightTheme,"light_theme")
        }
    }
}
