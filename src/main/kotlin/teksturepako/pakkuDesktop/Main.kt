/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop

import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowState
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import io.klogging.config.ANSI_CONSOLE
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RenderPattern
import io.klogging.sending.STDOUT
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debug
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.LocalPakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.window.MainWindow
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.application.theme.themedApplication
import teksturepako.pakkuDesktop.app.ui.driver.*
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.CloseDialogRequest
import teksturepako.pakkuDesktop.elm.run

fun main() {
    loggingConfiguration {
        ANSI_CONSOLE()
        sink(
            "console",
            RenderPattern("%-11t{LOCAL_TIME} %-5v{COLOUR} [%-10c] - %30l - %m %i%s"),
            STDOUT
        )
    }

    val credentials = runBlocking { CredentialsFile.readToResult() }
        .onFailure { error -> debug { println(error.rawMessage) } }
        .get()

    pakku {
        curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY") ?: credentials?.curseForgeApiKey)
        withUserAgent("PakkuDesktop (github.com/juraj-hrivnak/PakkuDesktop)")
    }

    // Load window data synchronously so the window can be sized correctly from the start
    val initialWindowData = runBlocking { WindowData.readOrNew() }

    // Bridge: lets onCloseRequest (outside composable tree) dispatch into the ELM loop
    var appPublish by mutableStateOf<((AppMsg) -> Unit)?>(null)

    themedApplication {
        MainWindow(
            initialWindowData = initialWindowData,
            onCloseRequest = {
                appPublish?.invoke(AppMsg.RequestCloseDialog(CloseDialogRequest.Quit(forceClose = true)))
                    ?: run {
                        // Fallback if publish not yet wired — just quit
                        kotlin.system.exitProcess(0)
                    }
            },
        ) { windowState ->
            CompositionLocalProvider(LocalPakkuApplicationScope provides this) {
                run(
                    appComponent,
                    drivers = listOf(
                        publishBridgeDriver { appPublish = it },
                        themeDriver,
                        profileDiskDriver,
                        modpackDiskDriver,
                        windowDiskDriver(
                            getWindowData = { snapshotWindowData(windowState) },
                            onQuit = {
                                exitApplication()
                                kotlin.system.exitProcess(0)
                            },
                        ),
                        directoryPickerDriver(),
                        licenseDriver,
                        actionDriver,
                    )
                )
            }
        }
    }
}

private fun snapshotWindowData(windowState: WindowState) = WindowData(
    placement = windowState.placement,
    x = windowState.position.x.value.takeUnless { it.isNaN() },
    y = windowState.position.y.value.takeUnless { it.isNaN() },
    width = windowState.size.width.value,
    height = windowState.size.height.value,
)

