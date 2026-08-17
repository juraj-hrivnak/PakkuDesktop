/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import com.github.michaelbull.result.get
import teksturepako.pakkuDesktop.app.actions.AdditionPlan
import teksturepako.pakkuDesktop.app.actions.RemovalPlan
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.component.dropdown.modpackDropdownComponent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredAndSortedProjects
import teksturepako.pakkuDesktop.app.ui.model.AddDialogModel
import teksturepako.pakkuDesktop.app.ui.model.AddDialogPhase
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsFabAction
import teksturepako.pakkuDesktop.app.ui.model.RemoveDialogModel
import teksturepako.pakkuDesktop.app.ui.model.RemoveDialogPhase
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pro.git.wrapper.gitFolderIds
import teksturepako.pakkuDesktop.pro.git.wrapper.mergeChangelistExpandedFolders
import teksturepako.pakkuDesktop.pro.ui.component.gitDropdownComponent

// -- modpackUpdate --

fun modpackUpdate(msg: ModpackMsg, model: ModpackModel): ModpackModel = when (msg) {

    // parent
    ModpackMsg.ShowSettings,
    ModpackMsg.ShowNewModpack,
    is ModpackMsg.CloseRequested,
    is ModpackMsg.DirectoryPicked -> model

    // Child-owned state

    is ModpackMsg.Loaded -> {
        val updatedSelectedProject = if (model.selectedProject != null) {
            val selectedKey = model.selectedProject.uiKey()
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
        )
    }

    ModpackMsg.Reset -> ModpackModel()

    is ModpackMsg.TabSelected        -> model.copy(selectedTab = msg.tab)
    is ModpackMsg.ProjectSelected    -> model.copy(selectedProject = msg.project, editingProject = false)
    is ModpackMsg.ProjectEditing     -> model.copy(editingProject = msg.editing)
    is ModpackMsg.ModpackEditing     -> model.copy(editingModpack = msg.editing)
    is ModpackMsg.ProjectsSelected   -> model.copy(selectedProjectKeys = model.selectedProjectKeys + msg.keys)
    is ModpackMsg.ProjectsDeselected -> model.copy(selectedProjectKeys = model.selectedProjectKeys - msg.keys)
    is ModpackMsg.ProjectsCleared    -> model.copy(selectedProjectKeys = emptySet())
    is ModpackMsg.ProjectsFabRemembered -> model.copy(lastProjectsFab = msg.action)
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
    is ModpackMsg.SortOrderChanged   -> model.copy(sortOrder = msg.order)
    is ModpackMsg.FilterTextChanged  -> model.copy(projectsFilterText = msg.text)
    is ModpackMsg.FilterUpdatesOnlyChanged -> model.copy(filterUpdatesOnly = msg.enabled)
    is ModpackMsg.FilterTypesChanged -> model.copy(filterTypes = msg.types)
    is ModpackMsg.FilterSidesChanged -> model.copy(filterSides = msg.sides)
    is ModpackMsg.FilterMissingSideChanged -> model.copy(filterMissingSide = msg.enabled)
    is ModpackMsg.FilterProvidersChanged -> model.copy(filterProviders = msg.providers)
    is ModpackMsg.FilterRedistributableChanged -> model.copy(filterRedistributable = msg.value)
    is ModpackMsg.ProjectsSplitRatioChanged -> model.copy(
        projectsSplitRatio = msg.ratio.coerceIn(0.05f, 0.95f),
    )

    ModpackMsg.FocusProjectsFilterRequested -> model.copy(wantsFocusProjectsFilter = true)
    ModpackMsg.FocusProjectsFilterConsumed -> model.copy(wantsFocusProjectsFilter = false)
    is ModpackMsg.ProjectsFilterFocusChanged -> model.copy(projectsFilterFocused = msg.focused)

    ModpackMsg.ShowAddDialog -> model.copy(
        addDialog = AddDialogModel(visible = true),
    )
    ModpackMsg.HideAddDialog -> model.copy(addDialog = AddDialogModel())
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
                acceptedRootIds = emptySet(),
                pendingResolveQuery = query,
            ),
        )
    }
    is ModpackMsg.AddResolveProgress -> model.copy(
        addDialog = model.addDialog.copy(resolveStatus = msg.status),
    )
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
                acceptedRootIds = accepted,
                pendingResolveQuery = null,
            ),
        )
    }
    is ModpackMsg.AddRootSelectionChanged -> model.copy(
        addDialog = model.addDialog.copy(acceptedRootIds = msg.acceptedRootIds),
    )
    ModpackMsg.AddBackToInput -> model.copy(
        addDialog = model.addDialog.copy(
            phase = AddDialogPhase.Input,
            resolveStatus = null,
            plan = null,
            acceptedRootIds = emptySet(),
            pendingResolveQuery = null,
        ),
    )
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
    ModpackMsg.HideRemoveDialog -> model.copy(removeDialog = RemoveDialogModel())
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
                acceptedProjectIds = acceptedRoots,
                acceptedDepIds = acceptedDeps,
                wantsBuildPlan = false,
            ),
        )
    }
    is ModpackMsg.RemoveRootSelectionChanged -> {
        val plan = model.removeDialog.plan
        val nextRoots = msg.acceptedProjectIds
        val prevRoots = model.removeDialog.acceptedProjectIds
        val added = nextRoots - prevRoots
        val removed = prevRoots - nextRoots
        var nextDeps = model.removeDialog.acceptedDepIds
        if (plan != null) {
            for (key in removed) {
                val childKeys = plan.projects.find { it.key == key }
                    ?.orphanedChildren?.map { it.key }?.toSet().orEmpty()
                nextDeps = nextDeps - childKeys
            }
            for (key in added) {
                val recommended = plan.projects.find { it.key == key }
                    ?.orphanedChildren
                    ?.filter { it.isRecommended }
                    ?.map { it.key }
                    ?.toSet()
                    .orEmpty()
                nextDeps = nextDeps + recommended
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
        removeDialog = model.removeDialog.copy(acceptedDepIds = msg.acceptedDepIds),
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
        val keys = model.filteredAndSortedProjects(projects).map { it.uiKey() }.toSet()
        model.copy(selectedProjectKeys = keys)
    }
    ModpackMsg.OpenDetailRequested -> {
        val projects = model.lockFile?.get()?.getAllProjects() ?: return model
        val key = model.selectedProjectKeys.firstOrNull()
            ?: model.selectedProject?.uiKey()
            ?: return model
        val project = projects.find { it.uiKey() == key } ?: return model
        model.copy(selectedProject = project, editingProject = false)
    }

    ModpackMsg.StatusCheckRequested -> model.copy(wantsStatusCheck = true)
    is ModpackMsg.StatusCheckCompleted -> model.copy(
        wantsStatusCheck = false,
        updatePreviews = msg.previews,
    )
    is ModpackMsg.UpdatesApplied -> {
        val previews = model.updatePreviews ?: return model
        if (msg.keys.isEmpty()) return model
        model.copy(
            updatePreviews = previews.mapValues { (id, info) ->
                if (id in msg.keys) info.copy(applied = true) else info
            },
        )
    }
    is ModpackMsg.UpdateFileSelected -> {
        val previews = model.updatePreviews ?: return model
        val info = previews[msg.projectKey] ?: return model
        val nextChanges = info.fileChanges.map { change ->
            if (change.providerShortName != msg.providerShortName) change
            else if (change.newFiles.none { (it.id.ifEmpty { it.fileName }) == msg.fileId }) change
            else change.copy(selectedFileId = msg.fileId)
        }
        model.copy(
            updatePreviews = previews + (msg.projectKey to info.copy(fileChanges = nextChanges)),
        )
    }

    ModpackMsg.ExportRequested -> model.copy(wantsExport = true)
    ModpackMsg.FetchRequested  -> model.copy(wantsFetch = true)
    is ModpackMsg.UpdateRequested -> model.copy(
        wantsUpdate = true,
        selectedProjectKeys = msg.keys.ifEmpty { model.selectedProjectKeys },
    )
    is ModpackMsg.AddRequested -> model.copy(pendingAddQuery = msg.query)
    is ModpackMsg.AddPlanConfirmed -> model.copy(pendingAdditionPlan = msg.plan)
    is ModpackMsg.RemoveRequested -> model.copy(pendingRemovalKeys = msg.keys)
    is ModpackMsg.RemovePlanConfirmed -> model.copy(pendingRemovalPlan = msg.plan)
    is ModpackMsg.InitRequested -> model.copy(wantsInit = true, pendingInitSpec = msg.spec)
    is ModpackMsg.FilesDropped -> model // handled at app level
    is ModpackMsg.ActionStarted -> model.copy(
        actionName = msg.name,
        wantsExport = false,
        wantsFetch = false,
        wantsUpdate = false,
        wantsStatusCheck = false,
        wantsTerminateAction = false,
        pendingAddQuery = null,
        pendingAdditionPlan = null,
        modpackDropdown = model.modpackDropdown.copy(actionEnabled = false),
    )
    ModpackMsg.ActionFinished -> model.copy(
        actionName = null,
        wantsTerminateAction = false,
        wantsExport = false,
        wantsFetch = false,
        wantsUpdate = false,
        modpackDropdown = model.modpackDropdown.copy(actionEnabled = true),
    )
    ModpackMsg.TerminateAction -> model.copy(wantsTerminateAction = true)
    ModpackMsg.MutationCompleted -> model.copy(
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
    ModpackMsg.PropertyWriteCompleted    -> model.copy(pendingPropertyWrite = null)

    is ModpackMsg.MetaWriteRequested -> model.copy(pendingMetaWrite = msg.request)
    ModpackMsg.MetaWriteCompleted    -> model.copy(pendingMetaWrite = null)

    is ModpackMsg.ToastAdded     -> model.copy(toasts = model.toasts + msg.toast)
    is ModpackMsg.ToastDismissed -> model.copy(toasts = model.toasts.filterNot { it.id == msg.id })

    is ModpackMsg.GitStateUpdated          -> {
        val incoming = msg.state
        val selectedPaths = model.git.selectedFiles.map { it.path }.toSet()
        val remapped = incoming.gitFiles.filter { it.path in selectedPaths }.toSet()
        val expanded = mergeChangelistExpandedFolders(
            model.git.expandedFolderPaths,
            model.git.gitFiles,
            incoming.gitFiles,
        )
        model.copy(
            git = incoming.copy(selectedFiles = remapped, expandedFolderPaths = expanded),
            gitDropdown = model.gitDropdown.copy(gitState = incoming),
        )
    }
    is ModpackMsg.GitFileSelectionToggled  -> {
        val g = model.git
        val sel =
            if (g.selectedFiles.any { it.path == msg.file.path }) {
                g.selectedFiles.filterNot { it.path == msg.file.path }.toSet()
            } else {
                g.selectedFiles.filterNot { it.path == msg.file.path }.toSet() + msg.file
            }
        model.copy(git = g.copy(selectedFiles = sel))
    }
    is ModpackMsg.GitFolderSelectionToggled -> {
        val g = model.git
        val prefix = msg.folderPath
        val under = g.gitFiles.filter { f ->
            f.path == prefix || f.path.startsWith("$prefix/")
        }.toSet()
        if (under.isEmpty()) model
        else {
            val underPaths = under.map { it.path }.toSet()
            val selectedPaths = g.selectedFiles.map { it.path }.toSet()
            val allSelected = underPaths.all { it in selectedPaths }
            // Path-based merge: GitFile equality includes mtime/size, so set +/- under
            // misses rows after refresh or disk touch — folder checkbox then appears broken.
            val sel =
                if (allSelected) {
                    g.selectedFiles.filterNot { it.path in underPaths }.toSet()
                } else {
                    g.selectedFiles.filterNot { it.path in underPaths }.toSet() + under
                }
            model.copy(git = g.copy(selectedFiles = sel))
        }
    }
    is ModpackMsg.GitChangelistFolderExpansionToggled -> {
        val g = model.git
        val ids = gitFolderIds(g.gitFiles)
        if (msg.folderPath !in ids) model
        else {
            val open = g.expandedFolderPaths
            val next = if (msg.folderPath in open) open - msg.folderPath else open + msg.folderPath
            model.copy(git = g.copy(expandedFolderPaths = next))
        }
    }
    ModpackMsg.GitChangelistExpandAllFolders -> {
        val g = model.git
        model.copy(git = g.copy(expandedFolderPaths = gitFolderIds(g.gitFiles)))
    }
    ModpackMsg.GitChangelistCollapseAllFolders -> model.copy(
        git = model.git.copy(expandedFolderPaths = emptySet()),
    )
    ModpackMsg.GitSelectAllChangedFiles    -> model.copy(
        git = model.git.copy(selectedFiles = model.git.gitFiles.toSet()),
    )
    ModpackMsg.GitClearChangedFileSelection -> model.copy(
        git = model.git.copy(selectedFiles = emptySet()),
    )
    is ModpackMsg.GitCommitMessageChanged  -> model.copy(git = model.git.copy(commitMessage = msg.message))
    is ModpackMsg.GitDiffFileSelected      -> model.copy(gitDiffPendingFile = msg.file)
    is ModpackMsg.GitDiffComputed          -> model.copy(gitCurrentDiff = msg.diff, gitDiffPendingFile = null)
    is ModpackMsg.GitEventProgressUpdated  -> model.copy(gitEventProgress = msg.progress)

    ModpackMsg.GitPullRequested     -> model.copy(wantsGitPull = true)
    ModpackMsg.GitPullFinished      -> model.copy(wantsGitPull = false, gitEventProgress = null)
    ModpackMsg.GitPushRequested     -> model.copy(wantsGitPush = true)
    ModpackMsg.GitPushFinished      -> model.copy(wantsGitPush = false, gitEventProgress = null)
    ModpackMsg.GitCommitRequested   -> model.copy(wantsGitCommit = true)
    is ModpackMsg.GitCommitFinished -> model.copy(
        wantsGitCommit = false,
        gitEventProgress = null,
        git = if (msg.success) {
            model.git.copy(commitMessage = "", selectedFiles = emptySet())
        } else {
            model.git
        },
    )
    is ModpackMsg.GitCheckoutRequested -> model.copy(gitCheckoutBranch = msg.branch)
    ModpackMsg.GitCheckoutFinished  -> model.copy(gitCheckoutBranch = null, gitEventProgress = null)

    is ModpackMsg.ModpackDropdown -> {
        val updatedDropdown = modpackDropdownComponent.update(msg.msg, model.modpackDropdown)
        val base = model.copy(modpackDropdown = updatedDropdown)
        when (msg.msg) {
            ModpackDropdownMsg.Export                -> base.copy(wantsExport = true)
            ModpackDropdownMsg.Fetch                 -> base.copy(wantsFetch = true)
            ModpackDropdownMsg.CheckUpdates          -> base.copy(wantsStatusCheck = true)
            // parent
            is ModpackDropdownMsg.CloseRequested,
            ModpackDropdownMsg.ShowSettings,
            is ModpackDropdownMsg.DirectoryPicked    -> model
        }
    }
    is ModpackMsg.GitDropdown -> {
        val updatedGitDropdown = gitDropdownComponent.update(msg.msg, model.gitDropdown)
        val base = model.copy(gitDropdown = updatedGitDropdown)
        when (msg.msg) {
            GitDropdownMsg.PullRequested              -> base.copy(wantsGitPull = true)
            GitDropdownMsg.PushRequested              -> base.copy(wantsGitPush = true)
            is GitDropdownMsg.TabSelected             -> base.copy(selectedTab = msg.msg.tab)
            is GitDropdownMsg.CheckoutRequested       -> base.copy(gitCheckoutBranch = msg.msg.branch)
            else                                      -> base
        }
    }
}

// -- modpackComponent --

val modpackComponent = component(
    init = ModpackModel(),
    update = ::modpackUpdate,
    // view in AppComponent
    view = { _, _ -> Spacer(Modifier) },
)