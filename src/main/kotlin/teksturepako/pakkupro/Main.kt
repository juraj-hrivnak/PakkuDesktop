package teksturepako.pakkupro

import teksturepako.pakku.debugMode
import teksturepako.pakkupro.ui.application.theme.themedApplication
import teksturepako.pakkupro.ui.application.window.MainWindow
import teksturepako.pakkupro.ui.view.RootView

fun main()
{
    // Set Pakku's debug mode to `true`
    debugMode = true

    themedApplication {
        MainWindow {
            RootView()
        }
    }
}
