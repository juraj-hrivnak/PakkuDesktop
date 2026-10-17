/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import com.github.michaelbull.result.get
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import teksturepako.pakkuDesktop.app.actions.AdditionPlan
import teksturepako.pakkuDesktop.app.actions.AppToast
import teksturepako.pakkuDesktop.app.actions.RemovalPlan
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.app.git.*
import teksturepako.pakkuDesktop.app.ui.component.dropdown.modpackDropdownComponent
import teksturepako.pakkuDesktop.app.ui.component.git.gitDropdownComponent
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackMetaField
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.*
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.withChild
import kotlin.collections.HashSet
import kotlin.collections.all
import kotlin.collections.any
import kotlin.collections.associateBy
import kotlin.collections.buildSet
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.indexOfFirst
import kotlin.collections.indices
import kotlin.collections.intersect
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mapTo
import kotlin.collections.minus
import kotlin.collections.none
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.collections.removeAll
import kotlin.collections.set
import kotlin.collections.toSet

// -- modpackUpdate --

fun modpackUpdate(msg: ModpackMsg, model: ModpackModel): ModpackModel = when (msg) {

    // parent
    ModpackMsg.ShowSettings,
    ModpackMsg.ShowNewModpack,
    is ModpackMsg.CloseRequested,
    is ModpackMsg.DirectoryPicked -> model

    // Child-owned state

    is ModpackMsg.Loaded -> {
        val selectedProject = model.selectedProject
        val updatedSelectedProject = if (selectedProject != null) {
            val selectedKey = selectedProject.uiKey()
            msg.lockFile.get()?.getAllProjects()?.find { p ->
                p.uiKey() == selectedKey
            }
        } else null
        model.copy(
            lockFile = msg.lockFile,
            configFile = msg.configFile,
            loaded = true,
            selectedProject = updatedSelectedProject,
            lockErrorDismissed = false,
            configErrorDismissed = false,
            updatePreviews = if (msg.retainUpdatePreviews) model.updatePreviews else null,
            filterUpdatesOnly = if (msg.retainUpdatePreviews) model.filterUpdatesOnly else false,
            filesPreviewMode = if (msg.retainUpdatePreviews) model.filesPreviewMode else FilesPreviewMode.None,
            fetchDownloads = if (msg.retainUpdatePreviews) model.fetchDownloads else persistentListOf(),
            fetchOverrides = if (msg.retainUpdatePreviews) model.fetchOverrides else persistentListOf(),
            fetchCleanup = if (msg.retainUpdatePreviews) model.fetchCleanup else persistentListOf(),
            filesHashMismatchKeys = if (msg.retainUpdatePreviews) model.filesHashMismatchKeys else persistentSetOf(),
            syncAdded = if (msg.retainUpdatePreviews) model.syncAdded else persistentListOf(),
            syncRemoved = if (msg.retainUpdatePreviews) model.syncRemoved else persistentListOf(),
            syncUpdated = if (msg.retainUpdatePreviews) model.syncUpdated else persistentListOf(),
            filesFilterChangeKinds = if (msg.retainUpdatePreviews) model.filesFilterChangeKinds else persistentSetOf(),
        )
    }

    is ModpackMsg.ManualOverridesLoaded -> model.copy(manualOverrides = msg.overrides.toPersistentList())

    is ModpackMsg.DiskSnapshotLoaded ->
        if (msg.snapshot == model.diskSnapshot) model else model.copy(diskSnapshot = msg.snapshot)

    ModpackMsg.Reset -> ModpackModel()

    is ModpackMsg.TabSelected        ->
        if (model.selectedTab == msg.tab) model else model.copy(selectedTab = msg.tab)
    is ModpackMsg.ProjectSelected    ->
        if (model.selectedProject == msg.project && !model.editingProject) model
        else model.copy(selectedProject = msg.project, editingProject = false)
    is ModpackMsg.ProjectEditing     ->
        if (model.editingProject == msg.editing) model else model.copy(editingProject = msg.editing)
    is ModpackMsg.ModpackEditing     ->
        if (model.editingModpack == msg.editing) model else model.copy(editingModpack = msg.editing)
    is ModpackMsg.ProjectsSelected   ->
        if (model.selectedProjectKeys.containsAll(msg.keys)) model
        else model.copy(selectedProjectKeys = model.selectedProjectKeys.addAll(msg.keys))
    is ModpackMsg.ProjectsDeselected ->
        if (msg.keys.none { it in model.selectedProjectKeys }) model
        else model.copy(selectedProjectKeys = model.selectedProjectKeys.removeAll(msg.keys))
    is ModpackMsg.ProjectsCleared    ->
        if (model.selectedProjectKeys.isEmpty()) model else model.copy(selectedProjectKeys = persistentSetOf())
    is ModpackMsg.ProjectsFabRemembered ->
        if (model.lastProjectsFab == msg.action) model else model.copy(lastProjectsFab = msg.action)
    is ModpackMsg.FilesFabRemembered ->
        if (model.lastFilesFab == msg.action) model else model.copy(lastFilesFab = msg.action)
    is ModpackMsg.GitFabRemembered ->
        if (model.lastGitFab == msg.action) model else model.copy(lastGitFab = msg.action)
    ModpackMsg.ActivateLastProjectsFab -> {
        fun canRun(action: ProjectsFabAction): Boolean = when (action) {
            ProjectsFabAction.Add -> model.actionName == null
            ProjectsFabAction.Update -> model.actionName == null
            ProjectsFabAction.Remove ->
                model.actionName == null &&
                    model.selectedProjectKeys.isNotEmpty() &&
                    model.lockFile?.get() != null
        }
        when (val fab = model.lastProjectsFab) {
            ProjectsFabAction.Add if canRun(fab) ->
                modpackUpdate(ModpackMsg.ShowAddDialog, model)
            ProjectsFabAction.Update if canRun(fab) ->
                if (model.selectedProjectKeys.isEmpty()) {
                    modpackUpdate(ModpackMsg.StatusCheckRequested, model)
                } else {
                    modpackUpdate(ModpackMsg.UpdateRequested(model.selectedProjectKeys), model)
                }
            ProjectsFabAction.Remove if canRun(fab) ->
                modpackUpdate(ModpackMsg.ShowRemoveDialog, model)
            else -> {
                // No remembered FAB, or it can't run — same as before: open detail.
                if (model.selectedProjectKeys.isNotEmpty() || model.selectedProject != null) {
                    modpackUpdate(ModpackMsg.OpenDetailRequested, model)
                } else model
            }
        }
    }
    ModpackMsg.ActivateLastFilesFab -> {
        val busy = model.actionName != null
        val hasLock = model.lockFile?.get() != null
        val files = model.filesTab()
        when (val fab = model.lastFilesFab) {
            FilesFabAction.Refresh if !busy && hasLock ->
                modpackUpdate(ModpackMsg.FetchDetectRequested, model)
            FilesFabAction.Sync if !busy && hasLock ->
                modpackUpdate(ModpackMsg.SyncDetectRequested, model)
            FilesFabAction.Fetch if files.canApplyFetch() ->
                modpackUpdate(ModpackMsg.FetchRequested, model)
            FilesFabAction.Add if files.canApplySync(SyncKind.Added) ->
                modpackUpdate(ModpackMsg.SyncApplyRequested(SyncKind.Added), model)
            FilesFabAction.Update if files.canApplySync(SyncKind.Updated) ->
                modpackUpdate(ModpackMsg.SyncApplyRequested(SyncKind.Updated), model)
            FilesFabAction.Remove if files.canApplySync(SyncKind.Removed) ->
                modpackUpdate(ModpackMsg.SyncApplyRequested(SyncKind.Removed), model)
            else -> model
        }
    }
    ModpackMsg.ActivateLastGitFab -> {
        val busy = model.gitBusy()
        val selected = model.git.selectedFiles.isNotEmpty()
        val canPush = model.git.defaultRemote != null
        when (model.lastGitFab) {
            GitFabAction.Pull if !busy ->
                modpackUpdate(ModpackMsg.ShowGitDialog(GitDialog.Update), model)
            GitFabAction.Push if !busy && canPush ->
                modpackUpdate(ModpackMsg.ShowGitDialog(GitDialog.Push), model)
            GitFabAction.Commit if !busy && selected ->
                modpackUpdate(ModpackMsg.ShowGitDialog(GitDialog.Commit), model)
            else -> model
        }
    }
    is ModpackMsg.SortOrderChanged   -> model.copy(sortOrder = msg.order)
    is ModpackMsg.FilterTextChanged  -> model.copy(projectsFilterText = msg.text)
    is ModpackMsg.FilterUpdatesOnlyChanged ->
        if (model.filterUpdatesOnly == msg.enabled) model else model.copy(filterUpdatesOnly = msg.enabled)
    is ModpackMsg.FilterTypesChanged ->
        if (model.filterTypes == msg.types) model else model.copy(filterTypes = msg.types.toPersistentSet())
    is ModpackMsg.FilterSidesChanged ->
        if (model.filterSides == msg.sides) model else model.copy(filterSides = msg.sides.toPersistentSet())
    is ModpackMsg.FilterMissingSideChanged ->
        if (model.filterMissingSide == msg.enabled) model else model.copy(filterMissingSide = msg.enabled)
    is ModpackMsg.FilterProvidersChanged ->
        if (model.filterProviders == msg.providers) model else model.copy(filterProviders = msg.providers.toPersistentSet())
    is ModpackMsg.FilterRedistributableChanged ->
        if (model.filterRedistributable == msg.value) model else model.copy(filterRedistributable = msg.value)
    is ModpackMsg.FilterSyncKindsChanged -> model
    is ModpackMsg.ProjectsSplitRatioChanged -> model.copy(
        projectsSplitRatio = msg.ratio.coerceIn(0.05f, 0.95f),
    )
    is ModpackMsg.FilesSplitRatioChanged -> model.copy(
        filesSplitRatio = msg.ratio.coerceIn(0.05f, 0.95f),
    )
    is ModpackMsg.FilesFilterTextChanged -> model.copy(filesFilterText = msg.text)
    is ModpackMsg.FilesFilterFoldersChanged ->
        if (model.filesFilterFolders == msg.folders) model else model.copy(filesFilterFolders = msg.folders.toPersistentSet())
    is ModpackMsg.FilesFilterChangeKindsChanged ->
        if (model.filesFilterChangeKinds == msg.kinds) model else model.copy(filesFilterChangeKinds = msg.kinds.toPersistentSet())
    is ModpackMsg.FilesFilterFocusChanged ->
        if (model.filesFilterFocused == msg.focused) model else model.copy(filesFilterFocused = msg.focused)
    is ModpackMsg.FilesFolderExpansionToggled -> {
        val collapsed = model.filesCollapsedFolders
        val next = if (msg.folderPath in collapsed) {
            collapsed.remove(msg.folderPath)
        } else {
            collapsed.add(msg.folderPath)
        }
        if (next == collapsed) model else model.copy(filesCollapsedFolders = next)
    }
    ModpackMsg.FilesExpandAllFolders ->
        if (model.filesCollapsedFolders.isEmpty()) model
        else model.copy(filesCollapsedFolders = persistentSetOf())
    is ModpackMsg.FilesCollapseAllFolders -> {
        val ids = msg.folderIds.toPersistentSet()
        if (ids == model.filesCollapsedFolders) model
        else model.copy(filesCollapsedFolders = ids)
    }

    ModpackMsg.FocusProjectsFilterRequested ->
        if (model.wantsFocusProjectsFilter) model else model.copy(wantsFocusProjectsFilter = true)
    ModpackMsg.FocusProjectsFilterConsumed ->
        if (!model.wantsFocusProjectsFilter) model else model.copy(wantsFocusProjectsFilter = false)
    is ModpackMsg.ProjectsFilterFocusChanged ->
        if (model.projectsFilterFocused == msg.focused) model else model.copy(projectsFilterFocused = msg.focused)

    ModpackMsg.ShowAddDialog -> model.copy(
        addDialog = AddDialogModel(visible = true),
    )
    ModpackMsg.HideAddDialog ->
        if (model.addDialog == AddDialogModel()) model else model.copy(addDialog = AddDialogModel())
    is ModpackMsg.AddQueryChanged -> model.copy(
        addDialog = model.addDialog.copy(query = msg.query),
    )
    ModpackMsg.AddResolveRequested -> {
        val query = model.addDialog.query
        if (query.isBlank()) model
        else model.copy(
            addDialog = model.addDialog.copy(
                phase = AddDialogPhase.Resolving,
                resolveStatus = "Resolving…",
                plan = null,
                acceptedRootIds = persistentSetOf(),
                pendingResolveQuery = query,
            ),
        )
    }
    is ModpackMsg.AddResolveProgress ->
        if (model.addDialog.resolveStatus == msg.status) model
        else model.copy(addDialog = model.addDialog.copy(resolveStatus = msg.status))
    is ModpackMsg.AddPlanBuilt -> {
        if (!model.addDialog.visible) return model
        val accepted = msg.plan.entries
            .filter { it.isRecommended }
            .map { it.key }
            .toSet()
        model.copy(
            addDialog = model.addDialog.copy(
                phase = AddDialogPhase.Review,
                resolveStatus = null,
                plan = msg.plan,
                acceptedRootIds = accepted.toPersistentSet(),
                pendingResolveQuery = null,
                pendingResolveProjects = null,
            ),
        )
    }
    is ModpackMsg.AddRootSelectionChanged -> model.copy(
        addDialog = model.addDialog.copy(acceptedRootIds = msg.acceptedRootIds.toPersistentSet()),
    )
    ModpackMsg.AddBackToInput -> {
        if (!model.addDialog.allowInput) return model.copy(addDialog = AddDialogModel())
        model.copy(
            addDialog = model.addDialog.copy(
                phase = AddDialogPhase.Input,
                resolveStatus = null,
                plan = null,
                acceptedRootIds = persistentSetOf(),
                pendingResolveQuery = null,
                pendingResolveProjects = null,
            ),
        )
    }
    ModpackMsg.AddConfirmRequested -> {
        val dialog = model.addDialog
        val plan = dialog.plan ?: return model
        val selected = plan.entries.filter { it.key in dialog.acceptedRootIds }
        if (selected.isEmpty()) return model
        model.copy(
            addDialog = AddDialogModel(),
            pendingAdditionPlan = AdditionPlan(selected, plan.messages),
        )
    }

    ModpackMsg.ShowRemoveDialog -> {
        // Keep update cheap so the dialog can paint immediately; exact cover is size equality.
        val packSize = model.lockFile?.get()?.getAllProjects()?.size ?: 0
        val removingAll = packSize > 0 && model.selectedProjectKeys.size == packSize
        if (removingAll) {
            model.copy(
                removeDialog = RemoveDialogModel(
                    visible = true,
                    phase = RemoveDialogPhase.ConfirmAll,
                    wantsBuildPlan = false,
                ),
            )
        } else {
            model.copy(
                removeDialog = RemoveDialogModel(
                    visible = true,
                    phase = RemoveDialogPhase.Loading,
                    wantsBuildPlan = true,
                ),
            )
        }
    }
    ModpackMsg.HideRemoveDialog ->
        if (model.removeDialog == RemoveDialogModel()) model else model.copy(removeDialog = RemoveDialogModel())
    ModpackMsg.RemoveAllAcknowledged -> {
        if (!model.removeDialog.visible) return model
        if (model.removeDialog.phase != RemoveDialogPhase.ConfirmAll) return model
        model.copy(
            removeDialog = model.removeDialog.copy(
                phase = RemoveDialogPhase.Loading,
                wantsBuildPlan = true,
            ),
        )
    }
    is ModpackMsg.RemovePlanBuilt -> {
        if (!model.removeDialog.visible) return model
        val acceptedRoots = msg.plan.projects
            .filter { it.isRecommended }
            .map { it.key }
            .toSet()
        // CLI defaults: orphaned deps yes, still-required deps no.
        val acceptedDeps = msg.plan.projects
            .filter { it.key in acceptedRoots }
            .flatMap { it.orphanedChildren }
            .filter { it.isRecommended }
            .map { it.key }
            .toSet()
        model.copy(
            removeDialog = model.removeDialog.copy(
                phase = RemoveDialogPhase.Ready,
                plan = msg.plan,
                acceptedProjectIds = acceptedRoots.toPersistentSet(),
                acceptedDepIds = acceptedDeps.toPersistentSet(),
                wantsBuildPlan = false,
            ),
        )
    }
    is ModpackMsg.RemoveRootSelectionChanged -> {
        val plan = model.removeDialog.plan
        val nextRoots = msg.acceptedProjectIds.toPersistentSet()
        val prevRoots = model.removeDialog.acceptedProjectIds
        val added = msg.acceptedProjectIds - prevRoots
        val removed = prevRoots - msg.acceptedProjectIds
        var nextDeps = model.removeDialog.acceptedDepIds
        if (plan != null) {
            for (key in removed) {
                val childKeys = plan.projects.find { it.key == key }
                    ?.orphanedChildren?.map { it.key }?.toSet().orEmpty()
                nextDeps = nextDeps.removeAll(childKeys)
            }
            for (key in added) {
                val recommended = plan.projects.find { it.key == key }
                    ?.orphanedChildren
                    ?.filter { it.isRecommended }
                    ?.map { it.key }
                    ?.toSet()
                    .orEmpty()
                nextDeps = nextDeps.addAll(recommended)
            }
        }
        model.copy(
            removeDialog = model.removeDialog.copy(
                acceptedProjectIds = nextRoots,
                acceptedDepIds = nextDeps,
            ),
        )
    }
    is ModpackMsg.RemoveDepSelectionChanged -> model.copy(
        removeDialog = model.removeDialog.copy(acceptedDepIds = msg.acceptedDepIds.toPersistentSet()),
    )
    ModpackMsg.RemoveConfirmRequested -> {
        val dialog = model.removeDialog
        val plan = dialog.plan ?: return model
        val roots = plan.projects.filter { it.key in dialog.acceptedProjectIds }
        if (roots.isEmpty()) return model
        val orphans = plan.orphansFor(dialog.acceptedProjectIds, dialog.acceptedDepIds)
        model.copy(
            removeDialog = RemoveDialogModel(),
            pendingRemovalPlan = RemovalPlan(
                projects = roots,
                orphanedDeps = orphans,
                messages = plan.messages,
            ),
        )
    }

    ModpackMsg.SelectAllFilteredRequested -> {
        val projects = model.lockFile?.get()?.getAllProjects() ?: return model
        val keys = if (model.selectedTab == SelectedTab.FILES) {
            model.filesTab().filteredFilesDiffPairs().filter { it.selectable }.map { it.selectionKey }.toSet()
        } else {
            model.projectsTab().filteredAndSortedProjects(projects).map { it.uiKey() }.toSet()
        }
        if (keys == model.selectedProjectKeys) model else model.copy(selectedProjectKeys = keys.toPersistentSet())
    }
    ModpackMsg.OpenDetailRequested -> {
        val key = model.selectedProjectKeys.firstOrNull()
            ?: model.selectedProject?.uiKey()
            ?: return model
        val project = model.lockFile?.get()?.getAllProjects()?.find { it.uiKey() == key } ?: return model
        if (model.selectedProject == project && !model.editingProject) model
        else model.copy(selectedProject = project, editingProject = false)
    }

    ModpackMsg.StatusCheckRequested -> model.copy(wantsStatusCheck = true)
    is ModpackMsg.StatusCheckCompleted -> model.copy(
        wantsStatusCheck = false,
        updatePreviews = msg.previews.toPersistentMap(),
    )
    is ModpackMsg.UpdatesApplied -> {
        val previews = model.updatePreviews ?: return model
        if (msg.keys.isEmpty()) return model
        model.copy(
            updatePreviews = previews.mutate { map ->
                for (key in msg.keys) {
                    map[key]?.let { map[key] = it.copy(applied = true) }
                }
            },
        )
    }
    is ModpackMsg.UpdateFileSelected -> {
        val previews = model.updatePreviews ?: return model
        val info = previews[msg.projectKey] ?: return model
        val nextChanges = info.fileChanges.map { change ->
            if (change.providerShortName != msg.providerShortName) change
            else if (change.selectedFileId == msg.fileId) change
            else if (change.newFiles.none { (it.id.ifEmpty { it.fileName }) == msg.fileId }) change
            else change.copy(selectedFileId = msg.fileId)
        }
        if (nextChanges.indices.all { nextChanges[it] === info.fileChanges[it] }) return model
        model.copy(
            updatePreviews = previews.put(msg.projectKey, info.copy(fileChanges = nextChanges)),
        )
    }

    ModpackMsg.ExportRequested -> model.copy(wantsExport = true)
    ModpackMsg.FetchDetectRequested -> model.copy(
        wantsFetchDetect = true,
        selectedTab = SelectedTab.FILES,
    )
    is ModpackMsg.FetchDetectCompleted -> {
        val kinds = buildSet {
            if (msg.result.toDownload.isNotEmpty()) add(FilesChangeKind.Fetch)
            if (msg.result.toCopyOverrides.isNotEmpty()) add(FilesChangeKind.Override)
            if (msg.result.toClean.isNotEmpty()) add(FilesChangeKind.Cleanup)
        }
        val keys = msg.result.selectionKeys()
        model.copy(
            wantsFetchDetect = false,
            filesPreviewMode = if (msg.result.isEmpty) FilesPreviewMode.None else FilesPreviewMode.Fetch,
            fetchDownloads = msg.result.toDownload.toPersistentList(),
            fetchOverrides = msg.result.toCopyOverrides.toPersistentList(),
            fetchCleanup = msg.result.toClean.toPersistentList(),
            filesHashMismatchKeys = msg.result.hashMismatchKeys.toPersistentSet(),
            manualOverrides = msg.result.allOverrides.toPersistentList(),
            syncAdded = persistentListOf(),
            syncRemoved = persistentListOf(),
            syncUpdated = persistentListOf(),
            filesFilterChangeKinds = kinds.toPersistentSet(),
            selectedProjectKeys = if (msg.result.isEmpty) persistentSetOf() else keys.toPersistentSet(),
            selectedTab = SelectedTab.FILES,
        )
    }
    ModpackMsg.FetchRequested -> model.copy(wantsFetch = true)
    is ModpackMsg.FetchProgressUpdated ->
        if (model.fetchProgressCompleted == msg.completed && model.fetchProgressTotal == msg.total) model
        else model.copy(
            fetchProgressCompleted = msg.completed,
            fetchProgressTotal = msg.total,
        )
    ModpackMsg.FilesPreviewCleared -> model.clearFilesPreview()
    ModpackMsg.SyncDetectRequested -> model.copy(
        wantsSyncDetect = true,
        selectedTab = SelectedTab.FILES,
    )
    is ModpackMsg.SyncDetectCompleted -> {
        val empty = msg.added.isEmpty() && msg.removed.isEmpty() && msg.updated.isEmpty()
        val kinds = buildSet {
            if (msg.added.isNotEmpty()) add(FilesChangeKind.Added)
            if (msg.removed.isNotEmpty()) add(FilesChangeKind.Removed)
            if (msg.updated.isNotEmpty()) add(FilesChangeKind.Updated)
        }
        val keys = (msg.added + msg.removed + msg.updated).map { it.uiKey() }.toSet()
        model.copy(
            wantsSyncDetect = false,
            filesPreviewMode = if (empty) FilesPreviewMode.None else FilesPreviewMode.Sync,
            fetchDownloads = persistentListOf(),
            fetchOverrides = persistentListOf(),
            fetchCleanup = persistentListOf(),
            filesHashMismatchKeys = persistentSetOf(),
            syncAdded = msg.added.toPersistentList(),
            syncRemoved = msg.removed.toPersistentList(),
            syncUpdated = msg.updated.toPersistentList(),
            filesFilterChangeKinds = kinds.toPersistentSet(),
            selectedProjectKeys = if (empty) persistentSetOf() else keys.toPersistentSet(),
            selectedTab = SelectedTab.FILES,
        )
    }
    is ModpackMsg.SyncApplyRequested -> {
        val pool = when (msg.kind) {
            SyncKind.Added -> model.syncAdded
            SyncKind.Removed -> model.syncRemoved
            SyncKind.Updated -> model.syncUpdated
        }
        val kindKeys = pool.map { it.uiKey() }.toSet()
        val keys = model.selectedProjectKeys.intersect(kindKeys)
        if (keys.isEmpty()) return model
        val selected = pool.filter { it.uiKey() in keys }
        val narrowed = model.copy(selectedProjectKeys = keys.toPersistentSet())
        when (msg.kind) {
            SyncKind.Added -> narrowed.copy(
                addDialog = AddDialogModel(
                    visible = true,
                    phase = AddDialogPhase.Resolving,
                    resolveStatus = "Resolving…",
                    pendingResolveProjects = selected.toPersistentList(),
                    allowInput = false,
                    confirmVerb = "Add",
                ),
            )
            SyncKind.Updated -> narrowed.copy(
                addDialog = AddDialogModel(
                    visible = true,
                    phase = AddDialogPhase.Resolving,
                    resolveStatus = "Resolving…",
                    pendingResolveProjects = selected.toPersistentList(),
                    allowInput = false,
                    confirmVerb = "Update",
                ),
            )
            SyncKind.Removed -> modpackUpdate(ModpackMsg.ShowRemoveDialog, narrowed)
        }
    }
    is ModpackMsg.UpdateRequested -> model.copy(
        wantsUpdate = true,
        selectedProjectKeys = if (msg.keys.isEmpty()) model.selectedProjectKeys else msg.keys.toPersistentSet(),
    )
    is ModpackMsg.AddRequested -> model.copy(pendingAddQuery = msg.query)
    is ModpackMsg.AddPlanConfirmed -> model.copy(pendingAdditionPlan = msg.plan)
    is ModpackMsg.RemoveRequested -> model.copy(pendingRemovalKeys = msg.keys.toPersistentSet())
    is ModpackMsg.RemovePlanConfirmed -> model.copy(pendingRemovalPlan = msg.plan)
    is ModpackMsg.InitRequested -> model.copy(wantsInit = true, pendingInitSpec = msg.spec)
    is ModpackMsg.FilesDropped -> model // handled at app level
    is ModpackMsg.ActionStarted -> model.copy(
        actionName = msg.name,
        wantsExport = false,
        wantsFetch = false,
        wantsFetchDetect = false,
        wantsUpdate = false,
        wantsStatusCheck = false,
        wantsSyncDetect = false,
        pendingSyncApply = false,
        wantsTerminateAction = false,
        pendingAddQuery = null,
        pendingAdditionPlan = null,
        modpackDropdown = model.modpackDropdown.copy(actionEnabled = false),
    )
    is ModpackMsg.ActionRenamed -> model.copy(actionName = msg.name)
    // Published twice on terminate (job finally + terminate effect).
    ModpackMsg.ActionFinished ->
        if (
            model.actionName == null && !model.wantsTerminateAction &&
            !model.wantsExport && !model.wantsFetch && !model.wantsUpdate &&
            model.fetchProgressCompleted == 0L && model.fetchProgressTotal == 0L &&
            model.modpackDropdown.actionEnabled
        ) model
        else model.copy(
            actionName = null,
            wantsTerminateAction = false,
            wantsExport = false,
            wantsFetch = false,
            wantsUpdate = false,
            fetchProgressCompleted = 0L,
            fetchProgressTotal = 0L,
            modpackDropdown = if (model.modpackDropdown.actionEnabled) model.modpackDropdown
                else model.modpackDropdown.copy(actionEnabled = true),
        )
    ModpackMsg.TerminateAction -> model.copy(wantsTerminateAction = true)
    // ActionStarted already cleared the add intents on the action paths.
    ModpackMsg.MutationCompleted ->
        if (
            model.pendingRemovalKeys == null && model.pendingRemovalPlan == null &&
            model.pendingAddQuery == null && model.pendingAdditionPlan == null &&
            !model.wantsInit && model.pendingInitSpec == null
        ) model
        else model.copy(
            pendingRemovalKeys = null,
            pendingRemovalPlan = null,
            pendingAddQuery = null,
            pendingAdditionPlan = null,
            wantsInit = false,
            pendingInitSpec = null,
        )
    ModpackMsg.DismissLockError -> model.copy(lockErrorDismissed = true)
    ModpackMsg.DismissConfigError -> model.copy(configErrorDismissed = true)

    is ModpackMsg.PropertyWriteRequested -> model.copy(pendingPropertyWrite = msg.request)
    ModpackMsg.PropertyWriteCompleted    ->
        if (model.pendingPropertyWrite == null) model else model.copy(pendingPropertyWrite = null)

    is ModpackMsg.MetaWriteRequested -> {
        val lock = model.lockFile?.get() ?: return@modpackUpdate model
        val config = model.configFile?.get() ?: return@modpackUpdate model
        val fieldKey = msg.request.fieldKey ?: ModpackMetaField.GENERAL
        val errors = msg.request.validate(config, lock)
        if (errors.isNotEmpty()) {
            model.copy(modpackMetaErrors = model.modpackMetaErrors.put(fieldKey, errors.toPersistentList()))
        } else {
            model.copy(
                pendingMetaWrite = msg.request,
                modpackMetaErrors = model.modpackMetaErrors.remove(fieldKey),
            )
        }
    }
    ModpackMsg.MetaWriteCompleted -> {
        val fieldKey = model.pendingMetaWrite?.fieldKey
        if (model.pendingMetaWrite == null) {
            model
        } else {
            model.copy(
                pendingMetaWrite = null,
                modpackMetaErrors = fieldKey?.let { model.modpackMetaErrors.remove(it) } ?: model.modpackMetaErrors,
            )
        }
    }
    is ModpackMsg.MetaWriteFailed -> {
        if (msg.errors.isEmpty() && model.pendingMetaWrite == null) return@modpackUpdate model
        val fieldKey = msg.fieldKey ?: ModpackMetaField.GENERAL
        model.copy(
            pendingMetaWrite = null,
            modpackMetaErrors = if (msg.errors.isEmpty()) {
                model.modpackMetaErrors
            } else {
                model.modpackMetaErrors.put(fieldKey, msg.errors.toPersistentList())
            },
        )
    }

    is ModpackMsg.ToastAdded     -> model.copy(
        toasts = model.toasts.mutate { toasts ->
            toasts.add(msg.toast)
            while (toasts.size > MAX_TOASTS) toasts.removeAt(0)
        },
    )
    // Auto-dismiss and manual dismiss can both fire for one toast.
    is ModpackMsg.ToastDismissed -> {
        val index = model.toasts.indexOfFirst { it.id == msg.id }
        if (index < 0) model else model.copy(toasts = model.toasts.removeAt(index))
    }

    is ModpackMsg.GitStateUpdated          -> gitStateUpdated(msg.state, model)
    is ModpackMsg.GitFileSelectionToggled  -> {
        val g = model.git
        val path = msg.file.path
        val removing = g.selectedFiles.any { it.path == path }
        val sel = g.selectedFiles.mutate { set ->
            set.removeAll { it.path == path }
            if (!removing) set.add(msg.file)
        }
        model.copy(git = g.copy(selectedFiles = sel))
    }
    is ModpackMsg.GitFolderSelectionToggled -> {
        val g = model.git
        val under = gitFilesUnderTreeFolder(g.gitFiles, msg.folderPath, model.gitGroupUnversioned).toSet()
        if (under.isEmpty()) model
        else {
            val underPaths = under.map { it.path }.toSet()
            val selectedPaths = g.selectedFiles.map { it.path }.toSet()
            val allSelected = underPaths.all { it in selectedPaths }
            // Path-based merge: GitFile equality includes mtime/size, so set +/- under
            // misses rows after refresh or disk touch — folder checkbox then appears broken.
            val sel = g.selectedFiles.mutate { set ->
                set.removeAll { it.path in underPaths }
                if (!allSelected) set.addAll(under)
            }
            model.copy(git = g.copy(selectedFiles = sel))
        }
    }
    is ModpackMsg.GitChangelistFolderExpansionToggled -> {
        val g = model.git
        val ids = gitFolderIds(g.gitFiles, model.gitGroupUnversioned)
        if (msg.folderPath !in ids) model
        else {
            val next = if (msg.folderPath in g.expandedFolderPaths) {
                g.expandedFolderPaths.remove(msg.folderPath)
            } else {
                g.expandedFolderPaths.add(msg.folderPath)
            }
            model.copy(git = g.copy(expandedFolderPaths = next))
        }
    }
    ModpackMsg.GitChangelistExpandAllFolders -> {
        val g = model.git
        val ids = gitFolderIds(g.gitFiles, model.gitGroupUnversioned).toPersistentSet()
        if (ids == g.expandedFolderPaths) model
        else model.copy(git = g.copy(expandedFolderPaths = ids))
    }
    ModpackMsg.GitChangelistCollapseAllFolders ->
        if (model.git.expandedFolderPaths.isEmpty()) model
        else model.copy(git = model.git.copy(expandedFolderPaths = persistentSetOf()))
    ModpackMsg.GitSelectAllChangedFiles    -> {
        val all = model.git.gitFiles.toPersistentSet()
        if (all == model.git.selectedFiles) model
        else model.copy(git = model.git.copy(selectedFiles = all))
    }
    ModpackMsg.GitClearChangedFileSelection ->
        if (model.git.selectedFiles.isEmpty()) model
        else model.copy(git = model.git.copy(selectedFiles = persistentSetOf()))
    is ModpackMsg.GitCommitMessageChanged  ->
        if (model.git.commitMessage == msg.message) model
        else model.copy(git = model.git.copy(commitMessage = msg.message))
    is ModpackMsg.GitDiffFileSelected      ->
        if (model.gitDiffPendingFile == msg.file) model else model.copy(gitDiffPendingFile = msg.file)
    is ModpackMsg.GitDiffComputed          ->
        if (model.gitCurrentDiff === msg.diff && model.gitDiffPendingFile == null) model
        else model.copy(gitCurrentDiff = msg.diff, gitDiffPendingFile = null)
    is ModpackMsg.GitEventProgressUpdated  ->
        if (model.gitEventProgress == msg.progress) model else model.copy(gitEventProgress = msg.progress)

    is ModpackMsg.ShowGitDialog ->
        if (model.gitBusy() || model.gitDialog == msg.dialog) model else model.copy(gitDialog = msg.dialog)
    ModpackMsg.HideGitDialog ->
        if (model.gitDialog == null) model else model.copy(gitDialog = null)

    is ModpackMsg.GitOpRequested ->
        if (model.gitBusy()) model
        else model.withPendingGitOp(msg.op).copy(
            // Dropping a stash keeps the unstash list open.
            gitDialog = if (msg.op is GitOp.DropStash) model.gitDialog else null,
        )
    is ModpackMsg.GitOpFinished ->
        if (model.pendingGitOp != msg.op) model
        else {
            val done = model.withPendingGitOp(null).copy(gitEventProgress = null)
            when {
                msg.success && msg.op is GitOp.Commit ->
                    done.copy(git = done.git.copy(commitMessage = "", selectedFiles = persistentSetOf()))
                msg.success && msg.op is GitOp.Push -> {
                    val op = msg.op as GitOp.Push
                    // After pushing tags, mark all local tags as pushed so they disappear from the Push dialog.
                    val git = if (op.tags) {
                        val localTagNames = done.git.refs.filter { it.type == GitRefType.Tag }.mapTo(mutableSetOf()) { it.name }
                        val updated = done.git.remoteTagNames.toMutableSet()
                        updated.addAll(localTagNames)
                        done.git.copy(remoteTagNames = updated)
                    } else done.git
                    done.copy(
                        git = git,
                        toasts = done.toasts.mutate { toasts ->
                            toasts.add(ToastData(body = AppToast.Message("Pushed successfully")))
                            while (toasts.size > MAX_TOASTS) toasts.removeAt(0)
                        },
                    )
                }
                else -> done
            }
        }
    is ModpackMsg.GitGroupUnversionedChanged ->
        if (model.gitGroupUnversioned == msg.grouped) model
        else model.copy(
            gitGroupUnversioned = msg.grouped,
            git = model.git.copy(
                expandedFolderPaths = gitFolderIds(model.git.gitFiles, msg.grouped).toPersistentSet(),
            ),
        )
    is ModpackMsg.GitFilterTextChanged ->
        if (model.gitFilterText == msg.text) model else model.copy(gitFilterText = msg.text)
    is ModpackMsg.GitLogCommitSelected ->
        if (model.gitLog.selectedHash == msg.hash) model
        else model.copy(
            gitLog = model.gitLog.copy(
                selectedHash = msg.hash,
                details = null,
                containingBranches = null,
                selectedChange = null,
                diff = null,
            ),
        )
    is ModpackMsg.GitLogVisibleRowsChanged ->
        if (model.gitLog.visibleRows == msg.rows) model
        else model.copy(gitLog = model.gitLog.copy(visibleRows = msg.rows))
    is ModpackMsg.GitLogFiltersChanged ->
        if (model.gitLog.filters == msg.filters) model
        else model.copy(gitLog = model.gitLog.copy(filters = msg.filters))
    is ModpackMsg.GitLogSubjectsLoaded -> {
        val log = model.gitLog
        val keep = msg.keep
        val dropsAny = keep != null && log.subjects.keys.any { it !in keep }
        val addsAny = msg.subjects.any { (hash, subject) -> log.subjects[hash] != subject }
        if (!dropsAny && !addsAny) model
        else {
            val subjects = log.subjects.mutate { map ->
                if (keep != null) map.keys.retainAll(keep)
                map.putAll(msg.subjects)
            }
            model.copy(gitLog = log.copy(subjects = subjects))
        }
    }
    is ModpackMsg.GitLogDetailsLoaded ->
        if (model.gitLog.selectedHash != msg.details.hash || model.gitLog.details == msg.details) model
        else model.copy(gitLog = model.gitLog.copy(details = msg.details))
    is ModpackMsg.GitLogContainingBranchesLoaded ->
        if (model.gitLog.selectedHash != msg.hash || model.gitLog.containingBranches == msg.branches) model
        else model.copy(gitLog = model.gitLog.copy(containingBranches = msg.branches.toPersistentList()))
    is ModpackMsg.GitLogChangeSelected ->
        if (model.gitLog.selectedChange == msg.change) model
        else model.copy(gitLog = model.gitLog.copy(selectedChange = msg.change, diff = null))
    is ModpackMsg.GitLogDiffComputed ->
        if (model.gitLog.selectedHash != msg.hash || model.gitLog.selectedChange != msg.change ||
            model.gitLog.diff == msg.diff
        ) model
        else model.copy(gitLog = model.gitLog.copy(diff = msg.diff))

    is ModpackMsg.ModpackDropdown -> {
        val updatedDropdown = modpackDropdownComponent.update(msg.msg, model.modpackDropdown)
        val base = model.withChild(model.modpackDropdown, updatedDropdown) { copy(modpackDropdown = it) }
        when (msg.msg) {
            ModpackDropdownMsg.Export                -> base.copy(wantsExport = true)
            ModpackDropdownMsg.Fetch                 -> base.copy(
                selectedTab = SelectedTab.FILES,
                wantsFetchDetect = true,
                lastFilesFab = FilesFabAction.Refresh,
            )
            ModpackDropdownMsg.Sync                  -> base.copy(
                selectedTab = SelectedTab.FILES,
                wantsSyncDetect = true,
                lastFilesFab = FilesFabAction.Sync,
            )
            ModpackDropdownMsg.CheckUpdates          -> base.copy(wantsStatusCheck = true)
            // parent
            is ModpackDropdownMsg.CloseRequested,
            ModpackDropdownMsg.ShowSettings,
            is ModpackDropdownMsg.DirectoryPicked    -> model
        }
    }
    is ModpackMsg.GitDropdown -> {
        val updatedGitDropdown = gitDropdownComponent.update(msg.msg, model.gitDropdown)
        val base = model.withChild(model.gitDropdown, updatedGitDropdown) { copy(gitDropdown = it) }
        when (val child = msg.msg) {
            is GitDropdownMsg.ShowDialog  -> modpackUpdate(ModpackMsg.ShowGitDialog(child.dialog), base)
            is GitDropdownMsg.OpRequested -> modpackUpdate(ModpackMsg.GitOpRequested(child.op), base)
            is GitDropdownMsg.TabSelected ->
                if (base.selectedTab == child.tab) base else base.copy(selectedTab = child.tab)
            is GitDropdownMsg.TagsToggled -> {
                val dd = base.gitDropdown
                if (dd.tagsExpanded == child.expanded) base
                else base.withChild(dd, dd.copy(tagsExpanded = child.expanded)) { copy(gitDropdown = it) }
            }
        }
    }
}

/** [value] when it differs from [old], else [old] itself, so identity-keyed caches stay valid. */
private fun <T> reuse(old: T, value: T): T = if (old == value) old else value

/** Repository fields of [incoming] on top of [this]; UI fields (message, selection, expansion) are kept. */
private fun GitState.withRepoFrom(incoming: GitState): GitState {
    val next = copy(
        gitFiles = reuse(gitFiles, incoming.gitFiles),
        branches = reuse(branches, incoming.branches),
        outgoingCommits = reuse(outgoingCommits, incoming.outgoingCommits),
        upstream = incoming.upstream,
        incomingCount = incoming.incomingCount,
        incomingChangedFiles = reuse(incomingChangedFiles, incoming.incomingChangedFiles),
        remotes = reuse(remotes, incoming.remotes),
        headMessage = incoming.headMessage,
        stashes = reuse(stashes, incoming.stashes),
        refs = reuse(refs, incoming.refs),
        remoteTagNames = reuse(remoteTagNames, incoming.remoteTagNames),
        headHash = incoming.headHash,
        // A refresh that omits presence (null) must not forget a check that already happened.
        isRepository = incoming.isRepository ?: isRepository,
        defaultBranch = if (incoming.isRepository == false) incoming.defaultBranch else defaultBranch,
    )
    return if (next == this) this else next
}

/**
 * A refresh rebuilds every list. Equal content must keep the old instances: the changelist tree
 * is cached against [GitState.gitFiles] by identity, and a fresh-but-equal list left the Commit
 * tab spinning forever after a branch switch.
 */
private fun gitStateUpdated(incoming: GitState, model: ModpackModel): ModpackModel {
    val prev = model.git
    val repo = prev.withRepoFrom(incoming)
    val filesChanged = repo.gitFiles !== prev.gitFiles

    val git = if (!filesChanged) repo else {
        val selectedPaths = prev.selectedFiles.mapTo(HashSet()) { it.path }
        val selected = repo.gitFiles.filter { it.path in selectedPaths }.toPersistentSet()
        val expanded = mergeChangelistExpandedFolders(
            prev.expandedFolderPaths,
            prev.gitFiles,
            repo.gitFiles,
            model.gitGroupUnversioned,
        ).toPersistentSet()
        repo.copy(
            selectedFiles = reuse(prev.selectedFiles, selected),
            expandedFolderPaths = reuse(prev.expandedFolderPaths, expanded),
        ).let { if (it == repo) repo else it }
    }

    // Keep the diff pane in step with the working tree.
    var currentDiff = model.gitCurrentDiff
    var pendingDiff = model.gitDiffPendingFile
    if (filesChanged) {
        val byPath = git.gitFiles.associateBy { it.path }
        pendingDiff?.let { pending -> pendingDiff = byPath[pending.path]?.let { reuse(pending, it) } }
        val diffPath = currentDiff?.newPath
        if (diffPath != null && pendingDiff == null) {
            val now = byPath[diffPath]
            val before = prev.gitFiles.firstOrNull { it.path == diffPath }
            when {
                now == null -> currentDiff = null
                now != before -> pendingDiff = now
            }
        }
    }

    val dropdownState = model.gitDropdown.gitState.withRepoFrom(incoming)
    val gitDropdown =
        if (dropdownState === model.gitDropdown.gitState) model.gitDropdown
        else model.gitDropdown.copy(gitState = dropdownState)

    return if (
        git === prev && gitDropdown === model.gitDropdown &&
        currentDiff === model.gitCurrentDiff && pendingDiff === model.gitDiffPendingFile
    ) {
        model
    } else {
        model.copy(
            git = git,
            gitDropdown = gitDropdown,
            gitCurrentDiff = currentDiff,
            gitDiffPendingFile = pendingDiff,
        )
    }
}

private fun ModpackModel.withPendingGitOp(op: GitOp?): ModpackModel {
    val busy = op != null
    return copy(
        pendingGitOp = op,
        gitDropdown = if (gitDropdown.busy == busy) gitDropdown else gitDropdown.copy(busy = busy),
    )
}

// -- modpackComponent --

val modpackComponent = component(
    init = ModpackModel(),
    update = ::modpackUpdate,
    // view in AppComponent
    view = { _, _ -> Spacer(Modifier) },
)