/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import androidx.compose.runtime.remember
import dev.nucleusframework.aot.runtime.AotRuntime
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.aotTraining
import dev.nucleusframework.application.nucleusApplication
import io.github.vinceglb.filekit.FileKit
import io.klogging.config.ANSI_INFO
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RenderPattern
import io.klogging.sending.STDOUT
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.pakku
import teksturepako.pakku.debug
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.plugins.PluginLoader
import teksturepako.pakkuDesktop.app.plugins.PluginRegistry
import teksturepako.pakkuDesktop.app.plugins.pluginDrivers
import teksturepako.pakkuDesktop.app.plugins.pluginsRoot
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.application.window.mainWindowDriver
import teksturepako.pakkuDesktop.app.ui.driver.*
import teksturepako.pakkuDesktop.elm.run
import kotlin.time.Duration.Companion.seconds

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("PakkuDesktop: uncaught exception on thread '${thread.name}'")
        throwable.printStackTrace()
    }

    if (System.getProperty("os.name").lowercase().contains("win")) {
        System.setProperty("skiko.renderApi", "OPENGL")
    }

    println(System.getenv("LD_LIBRARY_PATH"))

    loggingConfiguration {
        ANSI_INFO()
        sink(
            "console",
            RenderPattern("%-11t{LOCAL_TIME} %-5v{COLOUR} [%-10c] - %30l - %m %i%s"),
            STDOUT
        )
    }

    FileKit.init(appId = "PakkuDesktop")

    val aotTrainingRun = AotRuntime.isTraining()

    if (!aotTrainingRun) {
        val credentials = runBlocking { CredentialsFile.readToResult() }
            .onErr { error -> debug { println(error.rawMessage) } }
            .get()

        pakku {
            curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY") ?: credentials?.curseForgeApiKey)
            gitHub(accessToken = System.getenv("GITHUB_ACCESS_TOKEN") ?: credentials?.gitHubAccessToken)
            withUserAgent("PakkuDesktop (github.com/juraj-hrivnak/PakkuDesktop)")
        }
    } else {
        pakku {
            curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY"))
            gitHub(accessToken = System.getenv("GITHUB_ACCESS_TOKEN"))
            withUserAgent("PakkuDesktop (github.com/juraj-hrivnak/PakkuDesktop)")
        }
    }

    val initialWindowData =
        if (aotTrainingRun) WindowData() else runBlocking { WindowData.readOrNew() }
    val pluginsDir = pluginsRoot()
    val plugins =
        if (aotTrainingRun) PluginRegistry.Empty else PluginLoader.loadAll(pluginsDir)
    val rootComponent = appComponent(
        initialProjectsUi =
            if (aotTrainingRun) ProjectsUiData() else runBlocking { ProjectsUiData.readOrNew() },
        plugins = plugins,
        pluginsRoot = pluginsDir,
    )

    nucleusApplication(backend = NucleusBackend.Tao) {
        if (aotTrainingRun) {
            aotTraining(duration = 45.seconds)
        }

        val drivers = remember {
            listOf(
                themeDriver,
                mainWindowDriver(applicationScope = this, initialWindowData = initialWindowData),
                uiScaleDriver,
                themedBoxDriver,
                clipboardDriver,
                profileDiskDriver,
                projectsUiDiskDriver,
                modpackDiskDriver,
                modpackSummaryDriver,
                gitDriver,
                gitLogDriver,
                windowDiskDriver(
                    onQuit = {
                        exitApplication()
                        kotlin.system.exitProcess(0)
                    },
                    initialWindowData = initialWindowData,
                ),
                directoryPickerDriver(),
                actionDriver,
                projectEditDriver,
                projectMutationDriver,
                credentialsDriver,
                cloneDriver,
                heapHistogramDriver,
                heapFootprintDriver,
                updateDriver,
            ) + listOfNotNull(debugTickDriver.takeIf { isDebugTickEnabled }) + pluginDrivers(plugins)
        }
        run(rootComponent, drivers = drivers)
    }
}
