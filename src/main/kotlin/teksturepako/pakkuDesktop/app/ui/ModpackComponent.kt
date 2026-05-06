/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// modpackUpdate — pure update for ModpackModel.
// Cross-cutting messages (ShowSettings, CloseRequested, DirectoryPicked) leave
// the model unchanged; appUpdate handles the parent-level side.
// ---------------------------------------------------------------------------

fun modpackUpdate(msg: ModpackMsg, model: ModpackModel): ModpackModel = when (msg) {

    is ModpackMsg.Loaded -> {
        val updatedSelectedProject = if (model.selectedProject != null) {
            msg.lockFile.component1()?.getAllProjects()?.find { p ->
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

    // cross-cutting — parent handles
    ModpackMsg.ShowSettings       -> model
    ModpackMsg.ShowNewModpack     -> model
    is ModpackMsg.CloseRequested  -> model
    is ModpackMsg.DirectoryPicked -> model

    is ModpackMsg.TabSelected       -> model.copy(selectedTab = msg.tab)
    is ModpackMsg.ProjectSelected   -> model.copy(selectedProject = msg.project, editingProject = false)
    is ModpackMsg.ProjectEditing    -> model.copy(editingProject = msg.editing)
    is ModpackMsg.ProjectsSelected  -> model.copy(selectedPakkuIds = model.selectedPakkuIds + msg.pakkuIds)
    is ModpackMsg.ProjectsDeselected -> model.copy(selectedPakkuIds = model.selectedPakkuIds - msg.pakkuIds)
    is ModpackMsg.ProjectsCleared   -> model.copy(selectedPakkuIds = emptySet())
    is ModpackMsg.SortOrderChanged  -> model.copy(sortOrder = msg.order)
    is ModpackMsg.FilterTextChanged -> model.copy(projectsFilterText = msg.text)

    ModpackMsg.ExportRequested      -> model.copy(wantsExport = true)
    is ModpackMsg.ActionStarted     -> model.copy(
        actionName = msg.name, wantsExport = false, wantsTerminateAction = false
    )
    ModpackMsg.ActionFinished       -> model.copy(
        actionName = null, wantsTerminateAction = false, wantsExport = false
    )
    ModpackMsg.TerminateAction      -> model.copy(wantsTerminateAction = true)

    is ModpackMsg.ToastAdded        -> model.copy(toasts = model.toasts + msg.toast)
    is ModpackMsg.ToastDismissed    -> model.copy(toasts = model.toasts.filterNot { it.id == msg.id })
}

// ---------------------------------------------------------------------------
// modpackComponent
// ---------------------------------------------------------------------------

val modpackComponent = component(
    init = ModpackModel(),
    update = ::modpackUpdate,
    view = { publish, model ->
        val scope = LocalPakkuApplicationScope.current
        with(scope) { ModpackView(publish, model) }
    }
)


