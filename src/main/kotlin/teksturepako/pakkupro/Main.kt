package teksturepako.pakkupro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debugMode
import teksturepako.pakkupro.ui.application.theme.themedApplication
import teksturepako.pakkupro.ui.application.window.MainWindow
import teksturepako.pakkupro.ui.view.RootView
import teksturepako.pakkupro.ui.viewmodel.LicenseKeyViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel

fun main()
{
    // Set Pakku's debug mode to `true`
    debugMode = true

    pakku {
        developmentMode()
        withUserAgent("PakkuPro (github.com/juraj-hrivnak/PakkuPro)")
    }

    CoroutineScope(Dispatchers.IO).launch {
        ProfileViewModel.loadFromDisk()
        LicenseKeyViewModel.checkActivation()
    }

    themedApplication {
        MainWindow {
            RootView()
        }
    }
}
