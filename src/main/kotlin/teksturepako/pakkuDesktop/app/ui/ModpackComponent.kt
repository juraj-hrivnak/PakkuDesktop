/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import com.github.michaelbull.result.get
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.app.ui.component.dropdown.modpackDropdownComponent
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pro.git.wrapper.gitFolderIds
import teksturepako.pakkuDesktop.pro.ui.component.gitDropdownComponent
import teksturepako.pakkuDesktop.pro.git.wrapper.mergeChangelistExpandedFolders

// ---------------------------------------------------------------------------
// modpackUpdate — pure update for ModpackModel.
// Cross-cutting messages (ShowSettings, ShowNewModpack, CloseRequested,
// DirectoryPicked) are intentionally inert here — appUpdate / handleModpackMsg
// intercepts them at the parent level where AppModel is in scope.
// ---------------------------------------------------------------------------

fun modpackUpdate(msg: ModpackMsg, model: ModpackModel): ModpackModel = when (msg) {

    // Routed to parent (appUpdate) — child is intentionally inert
    ModpackMsg.ShowSettings,
    ModpackMsg.ShowNewModpack,
    is ModpackMsg.CloseRequested,
    is ModpackMsg.DirectoryPicked -> model

    // Child-owned state

    is ModpackMsg.Loaded -> {
        val updatedSelectedProject = if (model.selectedProject != null) {
            msg.lockFile.get()?.getAllProjects()?.find { p ->
                p.pakkuId == model.selectedProject.pakkuId
            }
        } else null
        model.copy(
            lockFile = msg.lockFile,
            configFile = msg.configFile,
            loaded = true,
            selectedProject = updatedSelectedProject,
        )
    }

    ModpackMsg.Reset -> ModpackModel()

    is ModpackMsg.TabSelected        -> model.copy(selectedTab = msg.tab)
    is ModpackMsg.ProjectSelected    -> model.copy(selectedProject = msg.project, editingProject = false)
    is ModpackMsg.ProjectEditing     -> model.copy(editingProject = msg.editing)
    is ModpackMsg.ProjectsSelected   -> model.copy(selectedPakkuIds = model.selectedPakkuIds + msg.pakkuIds)
    is ModpackMsg.ProjectsDeselected -> model.copy(selectedPakkuIds = model.selectedPakkuIds - msg.pakkuIds)
    is ModpackMsg.ProjectsCleared    -> model.copy(selectedPakkuIds = emptySet())
    is ModpackMsg.SortOrderChanged   -> model.copy(sortOrder = msg.order)
    is ModpackMsg.FilterTextChanged  -> model.copy(projectsFilterText = msg.text)

    ModpackMsg.ExportRequested -> model.copy(wantsExport = true)
    is ModpackMsg.ActionStarted -> model.copy(
        actionName = msg.name, wantsExport = false, wantsTerminateAction = false,
        modpackDropdown = model.modpackDropdown.copy(actionEnabled = false),
    )
    ModpackMsg.ActionFinished -> model.copy(
        actionName = null, wantsTerminateAction = false, wantsExport = false,
        modpackDropdown = model.modpackDropdown.copy(actionEnabled = true),
    )
    ModpackMsg.TerminateAction -> model.copy(wantsTerminateAction = true)

    is ModpackMsg.PropertyWriteRequested -> model.copy(pendingPropertyWrite = msg.request)
    ModpackMsg.PropertyWriteCompleted    -> model.copy(pendingPropertyWrite = null)

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
            // Cross-cutting — inert here; appUpdate handles CloseRequested & DirectoryPicked
            is ModpackDropdownMsg.CloseRequested,
            is ModpackDropdownMsg.DirectoryPicked    -> model
            else                                     -> base
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

// ---------------------------------------------------------------------------
// modpackComponent
// ---------------------------------------------------------------------------

val modpackComponent = component(
    init = ModpackModel(),
    update = ::modpackUpdate,
    // Real UI is composed from AppComponent (needs app-level AppMsg publish + PakkuApplicationScope).
    view = { _, _ -> Spacer(Modifier) },
)