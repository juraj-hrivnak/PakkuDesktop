package teksturepako.pakkuDesktop.ui.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import teksturepako.pakkuDesktop.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.ui.view.children.ModpackView
import teksturepako.pakkuDesktop.ui.view.children.WelcomeView
import teksturepako.pakkuDesktop.ui.viewmodel.ProfileViewModel

@Composable
@Preview
fun PakkuApplicationScope.RootView()
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    if (profileData.currentProfile == null)
    {
        WelcomeView()
    }
    else
    {
        ModpackView()
    }
}
