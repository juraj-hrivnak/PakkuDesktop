package teksturepako.pakkupro.ui.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.view.children.ActivationView
import teksturepako.pakkupro.ui.view.children.ModpackView
import teksturepako.pakkupro.ui.view.children.WelcomeView
import teksturepako.pakkupro.ui.viewmodel.LicenseKeyViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel

@Composable
@Preview
fun PakkuApplicationScope.RootView()
{
    if (LicenseKeyViewModel.isActivated == false)
    {
        ActivationView()
        return
    }

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
