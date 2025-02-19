package teksturepako.pakkupro

import io.klogging.config.ANSI_CONSOLE
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RenderPattern
import io.klogging.sending.STDOUT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import teksturepako.pakku.api.pakku
import teksturepako.pakkupro.ui.application.theme.themedApplication
import teksturepako.pakkupro.ui.application.window.MainWindow
import teksturepako.pakkupro.ui.view.RootView
import teksturepako.pakkupro.ui.viewmodel.LicenseKeyViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel

fun main()
{
    // Set Pakku's debug mode to `true`
//    debugMode = true

    loggingConfiguration {
        ANSI_CONSOLE()
        sink(
            "console",
            RenderPattern("%-11t{LOCAL_TIME} %-5v{COLOUR} [%-10c] - %30l - %m %i%s"),
            STDOUT
        )
    }

    pakku {
        developmentMode()
        withUserAgent("PakkuPro (github.com/juraj-hrivnak/PakkuPro)")
    }

    CoroutineScope(Dispatchers.IO).launch {
        LicenseKeyViewModel.checkActivation()
        ProfileViewModel.loadFromDisk()
    }

    themedApplication {
        MainWindow {
            RootView()
        }
    }
}
