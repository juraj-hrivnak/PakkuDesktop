/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.*
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// Composition Local — lets deeply nested views publish without prop-drilling
// ---------------------------------------------------------------------------

val LocalAppPublish = compositionLocalOf<(AppMsg) -> Unit> { {} }
val LocalAppModel   = compositionLocalOf<AppModel> { AppModel() }
val LocalPakkuApplicationScope = compositionLocalOf<PakkuApplicationScope> {
    error("No PakkuApplicationScope provided")
}

// ---------------------------------------------------------------------------
// Pure update function
// ---------------------------------------------------------------------------

fun appUpdate(msg: AppMsg, model: AppModel): AppModel = when (msg) {

    // -- Profile --

    is AppMsg.ProfileLoaded -> {
        val newModel = model.copy(
            profile = model.profile.copy(data = msg.data, loaded = true, pendingPath = null)
        )
        // If a profile is already set, navigate directly to Modpack
        if (msg.data.currentProfile != null && model.screen == AppScreen.Welcome) {
            newModel.copy(screen = AppScreen.Modpack, modpack = ModpackModel())
        } else newModel
    }

    is AppMsg.DirectoryPicked -> {
        // Record intent — profile disk driver will resolve name + save
        if (model.modpack.actionName != null) {
            // Action is running — show close dialog instead
            model.copy(closeDialog = CloseDialogRequest.OpenDirectory(msg.path))
        } else {
            model.copy(profile = model.profile.copy(pendingPath = msg.path))
        }
    }

    is AppMsg.ProfileCurrentResolved -> {
        model.copy(
            profile = model.profile.copy(data = msg.data, pendingPath = null),
            screen = if (msg.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome,
            modpack = ModpackModel(),
        )
    }

    is AppMsg.ThemeChangeRequested -> model // driver handles the write, then publishes ThemeChanged

    is AppMsg.ThemeChanged -> model.copy(
        profile = model.profile.copy(data = msg.data)
    )

    // -- Window --

    is AppMsg.WindowLoaded -> model.copy(
        window = model.window.copy(data = msg.data, loaded = true)
    )

    // -- Navigation --

    AppMsg.NavigateToWelcome -> model.copy(
        screen = AppScreen.Welcome,
        modpack = ModpackModel(),
        profile = model.profile.copy(
            data = model.profile.data.copy(currentProfile = null),
            pendingPath = null,
        )
    )

    AppMsg.ShowSettings -> model.copy(showSettings = true)
    AppMsg.HideSettings -> model.copy(showSettings = false)
    AppMsg.ShowNewModpack -> model.copy(showNewModpack = true)
    AppMsg.HideNewModpack -> model.copy(showNewModpack = false)

    // -- Close / Quit dialog --

    is AppMsg.RequestCloseDialog -> model.copy(closeDialog = msg.request)

    AppMsg.DismissCloseDialog -> model.copy(closeDialog = null)

    AppMsg.ConfirmCloseDialog -> when (val req = model.closeDialog) {
        is CloseDialogRequest.Quit -> model.copy(closeDialog = null, wantsQuit = true)
        is CloseDialogRequest.CloseModpack -> model.copy(
            closeDialog = null,
            screen = AppScreen.Welcome,
            modpack = ModpackModel(),
            profile = model.profile.copy(data = model.profile.data.copy(currentProfile = null))
        )
        is CloseDialogRequest.OpenDirectory -> model.copy(
            closeDialog = null,
            profile = model.profile.copy(pendingPath = req.path),
        )
        null -> model.copy(closeDialog = null)
    }

    AppMsg.QuitReady -> model.copy(wantsQuit = false) // driver already called exitApplication()

    // -- Pro --

    is AppMsg.ProActivationChecked -> model.copy(isProActivated = msg.activated)

    // -- Modpack --

    is AppMsg.Modpack -> appUpdateModpack(msg, model)
}

private fun appUpdateModpack(msg: AppMsg.Modpack, model: AppModel): AppModel {
    val modpack = model.modpack
    return when (msg) {
        is AppMsg.Modpack.Loaded -> {
            val updatedSelectedProject = if (modpack.selectedProject != null && msg.lockFile.component1() != null) {
                msg.lockFile.component1()?.getAllProjects()?.find { p ->
                    p.pakkuId == modpack.selectedProject.pakkuId
                }
            } else null
            model.copy(
                modpack = modpack.copy(
                    lockFile = msg.lockFile,
                    configFile = msg.configFile,
                    loaded = true,
                    selectedProject = updatedSelectedProject,
                )
            )
        }
        AppMsg.Modpack.Reset -> model.copy(modpack = ModpackModel())
        is AppMsg.Modpack.TabSelected -> model.copy(modpack = modpack.copy(selectedTab = msg.tab))
        is AppMsg.Modpack.ProjectSelected -> model.copy(modpack = modpack.copy(
            selectedProject = msg.project,
            editingProject = false,
        ))
        is AppMsg.Modpack.ProjectEditing -> model.copy(modpack = modpack.copy(editingProject = msg.editing))
        is AppMsg.Modpack.ProjectsSelected -> model.copy(modpack = modpack.copy(
            selectedPakkuIds = modpack.selectedPakkuIds + msg.pakkuIds
        ))
        is AppMsg.Modpack.ProjectsDeselected -> model.copy(modpack = modpack.copy(
            selectedPakkuIds = modpack.selectedPakkuIds - msg.pakkuIds
        ))
        is AppMsg.Modpack.ProjectsCleared -> model.copy(modpack = modpack.copy(selectedPakkuIds = emptySet()))
        is AppMsg.Modpack.SortOrderChanged -> model.copy(modpack = modpack.copy(sortOrder = msg.order))
        is AppMsg.Modpack.FilterTextChanged -> model.copy(modpack = modpack.copy(projectsFilterText = msg.text))
        AppMsg.Modpack.ExportRequested -> model.copy(modpack = modpack.copy(wantsExport = true))
        is AppMsg.Modpack.ActionStarted -> model.copy(modpack = modpack.copy(
            actionName = msg.name,
            wantsExport = false,
            wantsTerminateAction = false,
        ))
        AppMsg.Modpack.ActionFinished -> model.copy(modpack = modpack.copy(
            actionName = null,
            wantsTerminateAction = false,
            wantsExport = false,
        ))
        AppMsg.Modpack.TerminateAction -> model.copy(modpack = modpack.copy(wantsTerminateAction = true))
        is AppMsg.Modpack.ToastAdded -> model.copy(modpack = modpack.copy(
            toasts = modpack.toasts + msg.toast
        ))
        is AppMsg.Modpack.ToastDismissed -> model.copy(modpack = modpack.copy(
            toasts = modpack.toasts.filterNot { it.id == msg.id }
        ))
    }
}

// ---------------------------------------------------------------------------
// App component definition (view is declared inline below)
// ---------------------------------------------------------------------------

val appComponent = component(
    init = AppModel(),
    update = ::appUpdate,
    view = { publish, model ->
        CompositionLocalProvider(
            LocalAppPublish provides publish,
            LocalAppModel provides model,
        ) {
            val scope = LocalPakkuApplicationScope.current
            with(scope) { AppView(publish, model) }
        }
    }
)


