package teksturepako.pakkupro

import androidx.compose.runtime.rememberCoroutineScope
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debugMode
import teksturepako.pakkupro.ui.application.theme.themedApplication
import teksturepako.pakkupro.ui.application.window.MainWindow
import teksturepako.pakkupro.ui.view.RootView
import teksturepako.pakkupro.ui.viewmodel.LicenseKeyViewModel

fun main()
{
    // Set Pakku's debug mode to `true`
    debugMode = true

    pakku {
        developmentMode()
        withUserAgent("PakkuPro (github.com/juraj-hrivnak/PakkuPro)")
    }

    themedApplication {
        LicenseKeyViewModel.checkActivation(rememberCoroutineScope())

        MainWindow {
            RootView()
        }
    }
}
