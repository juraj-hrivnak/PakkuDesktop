/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import io.github.vinceglb.filekit.FileKit
import io.klogging.config.ANSI_CONSOLE
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RenderPattern
import io.klogging.sending.STDOUT
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debug
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.application.window.mainWindowDriver
import teksturepako.pakkuDesktop.app.ui.driver.*
import teksturepako.pakkuDesktop.elm.run

fun main() {
    println(System.getenv("LD_LIBRARY_PATH"))

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

    nucleusApplication(backend = NucleusBackend.Tao) {
        run(
            appComponent,
            drivers = listOf(
                themeDriver,
                mainWindowDriver(applicationScope = this),
                uiScaleDriver,
                themedBoxDriver,
                clipboardDriver,
                profileDiskDriver,
                projectsUiDiskDriver,
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
                projectMutationDriver,
                credentialsDriver,
                cloneDriver,
            ),
        )
    }
}
