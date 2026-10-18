/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import teksturepako.pakkuDesktop.app.git.NativeGit
import teksturepako.pakkuDesktop.app.actions.readConfigFileText
import teksturepako.pakkuDesktop.app.actions.toPreview
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.actions.walkManagedFiles
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.diskSnapshotFolderKeys
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.expectedDiskPaths
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredAndSortedProjects
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredFilesLinks
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.projectsListPrepKey
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.snapshotList
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.FetchOverridePreview
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.OverviewMode
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.model.filesTab
import teksturepako.pakkuDesktop.app.ui.model.projectsTab
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.elm.Driver
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path

// -- modpackDiskDriver --

val modpackDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestModel by rememberUpdatedState(model)
    val diskSnapshotNow = remember { AtomicReference(model.modpack.diskSnapshot) }
    diskSnapshotNow.set(model.modpack.diskSnapshot)

    LaunchedEffect(model.screen, model.profile.data.currentProfile) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val lockFile = LockFile.readToResult()
            val configFile = ConfigFile.readToResult()
            publish(AppMsg.Modpack(ModpackMsg.Loaded(lockFile, configFile)))
            // If the modpack directory has no lock file yet, open the New Modpack dialog.
            if (lockFile.getError() is FileNotFound) {
                publish(AppMsg.Modpack(ModpackMsg.ShowNewModpack))
            }
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
        withContext(Dispatchers.IO) {
            val overrides = readManualOverrides(model.modpack.configFile?.get()).map { it.toPreview() }
            publish(AppMsg.Modpack(ModpackMsg.ManualOverridesLoaded(overrides)))
        }
    }

    // Inputs changed: refresh disk state on whichever tab is open.
    LaunchedEffect(
        model.screen,
        model.modpack.lockFile,
        model.modpack.configFile,
        model.modpack.manualOverrides,
        model.modpack.actionName,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.actionName != null) return@LaunchedEffect
        val lock = model.modpack.lockFile?.get() ?: return@LaunchedEffect
        publishDiskSnapshot(publish, diskSnapshotNow, lock, model.modpack.configFile?.get(), model.modpack.manualOverrides)
    }

    // Entering Files re-lists the disk. Other tab switches do not: a walk plus applying
    // the snapshot was hitching every rail click.
    LaunchedEffect(model.screen, model.modpack.selectedTab == SelectedTab.FILES) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.selectedTab != SelectedTab.FILES) return@LaunchedEffect
        if (model.modpack.actionName != null) return@LaunchedEffect
        val lock = model.modpack.lockFile?.get() ?: return@LaunchedEffect
        publishDiskSnapshot(publish, diskSnapshotNow, lock, model.modpack.configFile?.get(), model.modpack.manualOverrides)
    }

    // pakku.json as stored on disk, after every load and again when the JSON view opens.
    LaunchedEffect(
        model.screen,
        model.modpack.configFile,
        model.modpack.overviewMode,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        val text = withContext(Dispatchers.IO) { readConfigFileText() }
        publish(AppMsg.Modpack(ModpackMsg.ConfigTextLoaded(text)))
    }

    // Same diff the Git tab shows: git diff HEAD -- pakku.json.
    LaunchedEffect(
        model.screen,
        model.profile.data.currentProfile?.path,
        model.modpack.overviewMode,
        model.modpack.overviewShowDiff,
        model.modpack.configFile,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.overviewMode != OverviewMode.Json || !model.modpack.overviewShowDiff) return@LaunchedEffect
        val path = model.profile.data.currentProfile?.path ?: return@LaunchedEffect
        val diff = withContext(Dispatchers.IO) {
            NativeGit.diffWorktree(File(path), ConfigFile.FILE_NAME)
        }
        publish(AppMsg.Modpack(ModpackMsg.ConfigDiffLoaded(diff)))
    }

    LaunchedEffect(model.screen, model.modpack.wantsConfigReload) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (!model.modpack.wantsConfigReload) return@LaunchedEffect
        val lockFile = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val configFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }
        publish(AppMsg.Modpack(ModpackMsg.Loaded(lockFile, configFile)))
        publish(AppMsg.Modpack(ModpackMsg.ConfigReloadCompleted))
    }

    var projectsPrep by remember { mutableStateOf<ProjectsListPrep?>(null) }
    LaunchedEffect(
        model.screen,
        model.modpack.lockFile,
        model.modpack.sortOrder,
        model.modpack.projectsFilterText,
        model.modpack.filterTypes,
        model.modpack.filterSides,
        model.modpack.filterMissingSide,
        model.modpack.filterProviders,
        model.modpack.filterRedistributable,
        model.modpack.filterUpdatesOnly,
        model.modpack.updatePreviews,
    ) {
        if (model.screen != AppScreen.Modpack) {
            projectsPrep = null
            return@LaunchedEffect
        }
        val tab = model.modpack.projectsTab()
        val key = tab.projectsListPrepKey()
        val prepared = withContext(Dispatchers.Default) {
            val all = tab.lockFile?.get()?.getAllProjects().snapshotList()
            val filtered = tab.filteredAndSortedProjects(all)
            ProjectsListPrep(key, all, filtered, filtered.map { it.uiKey() })
        }
        if (latestModel.modpack.projectsTab().projectsListPrepKey() != key) return@LaunchedEffect
        projectsPrep = prepared
    }

    var filesPrep by remember { mutableStateOf<FilesPanePrep?>(null) }
    LaunchedEffect(
        model.screen,
        model.modpack.lockFile,
        model.modpack.configFile,
        model.modpack.diskSnapshot,
        model.modpack.manualOverrides,
        model.modpack.filesPreviewMode,
        model.modpack.fetchDownloads,
        model.modpack.fetchOverrides,
        model.modpack.fetchCleanup,
        model.modpack.filesHashMismatchKeys,
        model.modpack.syncAdded,
        model.modpack.syncRemoved,
        model.modpack.syncUpdated,
        model.modpack.filesFilterText,
        model.modpack.filesFilterFolders,
        model.modpack.filesFilterChangeKinds,
    ) {
        if (model.screen != AppScreen.Modpack) {
            filesPrep = null
            return@LaunchedEffect
        }
        val tab = model.modpack.filesTab()
        val prepared = withContext(Dispatchers.Default) {
            val links = tab.filteredFilesLinks()
            val tree = PathTreeSnapshot.of(links) { it.path }
            val index = links.withIndex().associate { (i, link) -> link.pathKey to i }
            FilesPanePrep(
                links = links,
                tree = tree,
                selectableKeys = links.mapNotNull { it.selectionKey }.distinct(),
                linkIndexByPathKey = index,
            )
        }
        filesPrep = prepared
    }

    CompositionLocalProvider(
        LocalProjectsListPrep provides projectsPrep,
        LocalFilesPanePrep provides filesPrep,
    ) {
        content()
    }
}

private suspend fun publishDiskSnapshot(
    publish: (AppMsg) -> Unit,
    currentSnapshot: AtomicReference<DiskSnapshot>,
    lock: LockFile,
    config: ConfigFile?,
    overrides: List<FetchOverridePreview>,
) {
    val root = Path(workingPath)
    withContext(Dispatchers.IO) {
        val projects = lock.getAllProjects().snapshotList()
        val exclude = overrides.map { it.fullOutputPath }.toSet()
        val expected = expectedDiskPaths(projects, config, overrides)
        val folders = diskSnapshotFolderKeys(config, projects, overrides)
        val walked = walkManagedFiles(root, config, exclude)
        val snapshot = buildDiskSnapshot(root, expected, walked, folders)
        // Equality walks every path. Keep it off the UI thread, and skip the message
        // when nothing changed so a tab switch does not recompose the files tree.
        if (snapshot != currentSnapshot.get()) {
            publish(AppMsg.Modpack(ModpackMsg.DiskSnapshotLoaded(snapshot)))
        }
    }
}
