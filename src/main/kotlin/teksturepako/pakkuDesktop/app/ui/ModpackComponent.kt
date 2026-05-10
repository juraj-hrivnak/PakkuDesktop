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
import teksturepako.pakkuDesktop.elm.component

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
        actionName = msg.name, wantsExport = false, wantsTerminateAction = false
    )
    ModpackMsg.ActionFinished -> model.copy(
        actionName = null, wantsTerminateAction = false, wantsExport = false
    )
    ModpackMsg.TerminateAction -> model.copy(wantsTerminateAction = true)

    is ModpackMsg.PropertyWriteRequested -> model.copy(pendingPropertyWrite = msg.request)
    ModpackMsg.PropertyWriteCompleted    -> model.copy(pendingPropertyWrite = null)

    is ModpackMsg.ToastAdded     -> model.copy(toasts = model.toasts + msg.toast)
    is ModpackMsg.ToastDismissed -> model.copy(toasts = model.toasts.filterNot { it.id == msg.id })

    is ModpackMsg.GitStateUpdated          -> model.copy(git = msg.state)
    is ModpackMsg.GitFileSelectionToggled  -> {
        val g = model.git
        val sel = if (msg.file in g.selectedFiles) g.selectedFiles - msg.file else g.selectedFiles + msg.file
        model.copy(git = g.copy(selectedFiles = sel))
    }
    is ModpackMsg.GitCommitMessageChanged  -> model.copy(git = model.git.copy(commitMessage = msg.message))
    is ModpackMsg.GitDiffFileSelected      -> model.copy(gitDiffPendingFile = msg.file)
    is ModpackMsg.GitDiffComputed          -> model.copy(gitCurrentDiff = msg.diff, gitDiffPendingFile = null)
    is ModpackMsg.GitEventProgressUpdated  -> model.copy(gitEventProgress = msg.progress)

    ModpackMsg.GitPullRequested     -> model.copy(wantsGitPull = true)
    ModpackMsg.GitPullFinished      -> model.copy(wantsGitPull = false, gitEventProgress = null)
    ModpackMsg.GitPushRequested     -> model.copy(wantsGitPush = true)
    ModpackMsg.GitPushFinished      -> model.copy(wantsGitPush = false, gitEventProgress = null)
    ModpackMsg.GitCommitRequested   -> model.copy(wantsGitCommit = true)
    ModpackMsg.GitCommitFinished    -> model.copy(wantsGitCommit = false, gitEventProgress = null)
    is ModpackMsg.GitCheckoutRequested -> model.copy(gitCheckoutBranch = msg.branch)
    ModpackMsg.GitCheckoutFinished  -> model.copy(gitCheckoutBranch = null, gitEventProgress = null)
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