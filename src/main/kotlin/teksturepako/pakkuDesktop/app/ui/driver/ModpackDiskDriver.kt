/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.readManualOverrides
import teksturepako.pakkuDesktop.app.actions.buildDiskSnapshot
import teksturepako.pakkuDesktop.app.actions.toPreview
import teksturepako.pakkuDesktop.app.actions.walkManagedFiles
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.diskSnapshotFolderKeys
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.expectedDiskPaths
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.snapshotList
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import kotlin.io.path.Path

// -- modpackDiskDriver --

val modpackDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.screen, model.profile.data.currentProfile) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect

        val lockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val configFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack(ModpackMsg.Loaded(lockFile, configFile)))

        // If the modpack directory has no lock file yet, open the New Modpack dialog.
        if (lockFile.getError() is FileNotFound) {
            publish(AppMsg.Modpack(ModpackMsg.ShowNewModpack))
        }
    }

    LaunchedEffect(
        model.screen,
        model.modpack.lockFile,
        model.modpack.configFile,
        model.modpack.actionName,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.actionName != null) return@LaunchedEffect
        val overrides = withContext(Dispatchers.IO) {
            readManualOverrides(model.modpack.configFile?.get()).map { it.toPreview() }
        }
        publish(AppMsg.Modpack(ModpackMsg.ManualOverridesLoaded(overrides)))
    }

    // selectedTab: a tab switch re-lists the disk without a fetch scan.
    LaunchedEffect(
        model.screen,
        model.modpack.selectedTab,
        model.modpack.lockFile,
        model.modpack.configFile,
        model.modpack.manualOverrides,
        model.modpack.actionName,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.actionName != null) return@LaunchedEffect
        val lock = model.modpack.lockFile?.get() ?: return@LaunchedEffect
        val config = model.modpack.configFile?.get()
        val overrides = model.modpack.manualOverrides
        val root = Path(workingPath)
        val projects = lock.getAllProjects().snapshotList()
        val exclude = overrides.map { it.fullOutputPath }.toSet()
        val snapshot = withContext(Dispatchers.IO) {
            val expected = expectedDiskPaths(projects, config, overrides)
            val folders = diskSnapshotFolderKeys(config, projects, overrides)
            val walked = walkManagedFiles(root, config, exclude)
            buildDiskSnapshot(root, expected, walked, folders)
        }
        publish(AppMsg.Modpack(ModpackMsg.DiskSnapshotLoaded(snapshot)))
    }

    content()
}
