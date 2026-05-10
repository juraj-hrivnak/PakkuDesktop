/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop

import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import io.github.vinceglb.filekit.FileKit
import io.klogging.config.ANSI_CONSOLE
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RenderPattern
import io.klogging.sending.STDOUT
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debug
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.application.window.mainWindowDriver
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

    FileKit.init(appId = "PakkuDesktop")

    val credentials = runBlocking { CredentialsFile.readToResult() }
        .onFailure { error -> debug { println(error.rawMessage) } }
        .get()

    pakku {
        curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY") ?: credentials?.curseForgeApiKey)
        withUserAgent("PakkuDesktop (github.com/juraj-hrivnak/PakkuDesktop)")
    }

    val initialWindowData = runBlocking { WindowData.readOrNew() }

    var appPublish by mutableStateOf<((AppMsg) -> Unit)?>(null)

    application {
        run(
            appComponent,
            drivers = listOf(
                // IntUiTheme must wrap JewelDecoratedWindow — same as old themedApplication { MainWindow { } }
                themeDriver,
                mainWindowDriver(
                    applicationScope = this@application,
                    initialWindowData = initialWindowData,
                    onCloseRequest = {
                        appPublish?.invoke(AppMsg.RequestCloseDialog(CloseDialogRequest.Quit(forceClose = true)))
                            ?: run {
                                kotlin.system.exitProcess(0)
                            }
                    },
                ),
                publishBridgeDriver { appPublish = it },
                themedBoxDriver,
                profileDiskDriver,
                modpackDiskDriver,
                gitDriver,
                windowDiskDriver(
                    onQuit = {
                        exitApplication()
                        kotlin.system.exitProcess(0)
                    },
                ),
                directoryPickerDriver(),
                licenseDriver,
                actionDriver,
                projectEditDriver,
            ),
        )
    }
}
