/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontFamily
import com.github.michaelbull.result.get
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.data.Profile
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pro.data.Polar
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

private val logger = logger("Drivers")

// ---------------------------------------------------------------------------
// CompositionLocals provided by drivers
// ---------------------------------------------------------------------------

/** Provides a function that launches the directory picker. */
val LocalPickDirectory = compositionLocalOf<() -> Unit> { {} }

/** Provides a function to launch a named action (suspend block). */
val LocalLaunchAction = compositionLocalOf<(name: String, block: suspend () -> Unit) -> Unit> { { _, _ -> } }

// ---------------------------------------------------------------------------
// publishBridgeDriver — wires the window's onCloseRequest to the ELM loop
// ---------------------------------------------------------------------------

fun publishBridgeDriver(onPublish: ((AppMsg) -> Unit) -> Unit): Driver<AppModel, AppMsg> =
    { publish, _, content ->
        SideEffect { onPublish(publish) }
        content()
    }

// ---------------------------------------------------------------------------
// themeDriver — wraps content with the correct IntUiTheme
// ---------------------------------------------------------------------------

val themeDriver: Driver<AppModel, AppMsg> = { _, model, content ->
    val textStyle = JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default)

    val themeDefinition = if (model.profile.data.intUiTheme.isDark()) {
        JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
    } else {
        JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
    }

    IntUiTheme(themeDefinition, ComponentStyling.default(), swingCompatMode = false) {
        content()
    }
}

// ---------------------------------------------------------------------------
// profileDiskDriver — loads profile on startup, saves when changed
// ---------------------------------------------------------------------------

val profileDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    // Load once on startup
    LaunchedEffect(Unit) {
        val data = withContext(Dispatchers.IO) { ProfileData.readOrNew() }
        publish(AppMsg.ProfileLoaded(data))
    }

    // React to pending path (user picked a directory)
    LaunchedEffect(model.profile.pendingPath, model.profile.loaded) {
        val pending = model.profile.pendingPath ?: return@LaunchedEffect
        if (!model.profile.loaded) return@LaunchedEffect

        val path: Path = Path(pending)

        withContext(Dispatchers.IO) {
            val modpackName = ConfigFile.readToResultFrom(
                Path("$path/${ConfigFile.FILE_NAME}")
            ).get()?.getName() ?: path.fileName.pathString

            val newProfile = Profile(
                name       = modpackName,
                path       = path.absolutePathString(),
                lastOpened = Clock.System.now()
            )

            val existing = model.profile.data
            val updatedRecentProfiles = buildList {
                // Add / update in recent list
                val existingIdx = existing.recentProfiles.indexOfFirst { it.path == newProfile.path }
                if (existingIdx >= 0) {
                    addAll(existing.recentProfiles.toMutableList().also { it[existingIdx] = newProfile })
                } else {
                    addAll(existing.recentProfiles + newProfile)
                }
            }

            val withOldCurrentInRecents = if (existing.currentProfile != null &&
                updatedRecentProfiles.none { it.path == existing.currentProfile.path }
            ) {
                updatedRecentProfiles + existing.currentProfile
            } else updatedRecentProfiles

            val newData = existing.copy(
                currentProfile = newProfile,
                recentProfiles = withOldCurrentInRecents,
            )

            workingPath = path.absolutePathString()
            logger.info { "workingPath set to [$workingPath]" }
            newData.write()

            withContext(Dispatchers.Main) {
                publish(AppMsg.ProfileCurrentResolved(newData))
            }
        }
    }

    // Navigate to Welcome when current profile is cleared (driver saves the cleared state)
    LaunchedEffect(model.profile.data.currentProfile, model.profile.loaded) {
        if (!model.profile.loaded) return@LaunchedEffect
        if (model.profile.data.currentProfile == null && model.screen == AppScreen.Welcome) {
            // Save the cleared profile to disk
            withContext(Dispatchers.IO) {
                workingPath = "."
                model.profile.data.write()
            }
        }
    }

    // Handle theme change requests
    LaunchedEffect(model.profile.data.theme, model.profile.loaded) {
        if (!model.profile.loaded) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            model.profile.data.write()
        }
    }

    content()
}

// ---------------------------------------------------------------------------
// modpackDiskDriver — loads LockFile + ConfigFile when on Modpack screen
// ---------------------------------------------------------------------------

val modpackDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.screen, model.profile.data.currentProfile) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect

        val lockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val configFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack.Loaded(lockFile, configFile))
    }

    content()
}

// ---------------------------------------------------------------------------
// windowDiskDriver — loads window data once; saves + quits when wantsQuit
// ---------------------------------------------------------------------------

fun windowDiskDriver(
    getWindowData: () -> teksturepako.pakkuDesktop.app.data.WindowData,
    onQuit: () -> Unit,
): Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(Unit) {
        val data = withContext(Dispatchers.IO) {
            teksturepako.pakkuDesktop.app.data.WindowData.readOrNew()
        }
        publish(AppMsg.WindowLoaded(data))
    }

    LaunchedEffect(model.wantsQuit) {
        if (!model.wantsQuit) return@LaunchedEffect
        withContext(Dispatchers.IO) { getWindowData().write() }
        onQuit()
        publish(AppMsg.QuitReady)
    }

    content()
}

// ---------------------------------------------------------------------------
// directoryPickerDriver — provides LocalPickDirectory CompositionLocal
// ---------------------------------------------------------------------------

fun directoryPickerDriver(): Driver<AppModel, AppMsg> = { publish, _, content ->
    val launcher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
    ) { directory ->
        directory?.path?.let { publish(AppMsg.DirectoryPicked(it)) }
    }

    CompositionLocalProvider(LocalPickDirectory provides { launcher.launch() }) {
        content()
    }
}

// ---------------------------------------------------------------------------
// licenseDriver — checks pro activation once on startup
// ---------------------------------------------------------------------------

val licenseDriver: Driver<AppModel, AppMsg> = { publish, _, content ->
    LaunchedEffect(Unit) {
        val activated = withContext(Dispatchers.IO) { Polar.isActivated() }
        publish(AppMsg.ProActivationChecked(activated))
    }
    content()
}

// ---------------------------------------------------------------------------
// actionDriver — owns the running action coroutine
// ---------------------------------------------------------------------------

val actionDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestPublish by rememberUpdatedState(publish)
    val latestModel   by rememberUpdatedState(model)
    val coroutineScope = rememberCoroutineScope()
    val currentJob = remember { mutableStateOf<Job?>(null) }

    val launchAction: (String, suspend () -> Unit) -> Unit = remember {
        { name, block ->
            if (currentJob.value?.isActive != true) {
                currentJob.value = coroutineScope.launch {
                    latestPublish(AppMsg.Modpack.ActionStarted(name))
                    try {
                        withContext(Dispatchers.IO) { block() }
                    } finally {
                        currentJob.value = null
                        latestPublish(AppMsg.Modpack.ActionFinished)
                    }
                }
            }
        }
    }

    // React to export intent
    LaunchedEffect(model.modpack.wantsExport) {
        if (!model.modpack.wantsExport) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        launchAction("Exporting") {
            teksturepako.pakkuDesktop.app.actions.exportSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack.ToastAdded(toast))
                }
            }
        }
    }

    // React to terminate request
    LaunchedEffect(model.modpack.wantsTerminateAction) {
        if (!model.modpack.wantsTerminateAction) return@LaunchedEffect
        currentJob.value?.cancelAndJoin()
        currentJob.value = null
        latestPublish(AppMsg.Modpack.ActionFinished)
    }

    CompositionLocalProvider(LocalLaunchAction provides launchAction) {
        content()
    }
}









