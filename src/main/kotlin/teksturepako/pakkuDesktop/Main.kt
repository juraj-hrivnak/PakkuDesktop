/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop

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
import teksturepako.pakku.debugMode
import teksturepako.pakkuDesktop.app.ui.application.theme.themedApplication
import teksturepako.pakkuDesktop.app.ui.application.window.MainWindow
import teksturepako.pakkuDesktop.app.ui.view.RootView
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.WindowViewModel
import teksturepako.pakkuDesktop.pro.ui.viewmodel.LicenseKeyViewModel

fun main()
{
    // Set Pakku's debug mode to `true`
    debugMode = false

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

    runBlocking {
        WindowViewModel.loadFromDisk()
        LicenseKeyViewModel.checkActivation()
        ProfileViewModel.loadFromDisk()
    }

    themedApplication {
        MainWindow {
            RootView()
        }
    }
}
