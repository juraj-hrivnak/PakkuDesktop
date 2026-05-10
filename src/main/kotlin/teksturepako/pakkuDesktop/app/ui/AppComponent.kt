/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.app.ui.view.routes.WelcomeView
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.NewModpackDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// appUpdate handlers — grouped by concern
// ---------------------------------------------------------------------------

private fun handleProfileMsg(msg: AppMsg, model: AppModel): AppModel = when (msg) {
    is AppMsg.ProfileLoaded -> {
        val newProfile = model.profile.copy(data = msg.data, loaded = true, pendingPath = null)
        val syncedWelcome = model.welcome.copy(profileData = msg.data)
        val base = model.copy(profile = newProfile, welcome = syncedWelcome)
        if (msg.data.currentProfile != null && model.screen == AppScreen.Welcome) {
            base.copy(screen = AppScreen.Modpack, modpack = ModpackModel())
        } else base
    }

    is AppMsg.ProfileCurrentResolved -> {
        val syncedWelcome = model.welcome.copy(profileData = msg.data)
        model.copy(
            profile = model.profile.copy(data = msg.data, pendingPath = null),
            screen = if (msg.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome,
            modpack = ModpackModel(),
            welcome = syncedWelcome,
        )
    }

    is AppMsg.ThemeChangeRequested -> {
        val newData = model.profile.data.copy(theme = msg.theme.toString())
        val syncedWelcome = model.welcome.copy(profileData = newData)
        model.copy(profile = model.profile.copy(data = newData), welcome = syncedWelcome)
    }

    is AppMsg.ThemeChanged -> {
        val syncedWelcome = model.welcome.copy(profileData = msg.data)
        model.copy(profile = model.profile.copy(data = msg.data), welcome = syncedWelcome)
    }

    else -> model
}

private fun handleDirectoryPicked(path: String, model: AppModel): AppModel =
    if (model.modpack.actionName != null) {
        model.copy(closeDialog = CloseDialogRequest.OpenDirectory(path))
    } else {
        model.copy(profile = model.profile.copy(pendingPath = path))
    }

private fun handleCloseDialogMsg(msg: AppMsg, model: AppModel): AppModel = when (msg) {
    is AppMsg.RequestCloseDialog -> model.copy(closeDialog = msg.request)
    AppMsg.DismissCloseDialog    -> model.copy(closeDialog = null)

    AppMsg.ConfirmCloseDialog -> when (val req = model.closeDialog) {
        is CloseDialogRequest.Quit         -> model.copy(closeDialog = null, wantsQuit = true)
        is CloseDialogRequest.CloseModpack -> model.copy(
            closeDialog = null,
            screen = AppScreen.Welcome,
            modpack = ModpackModel(),
            profile = model.profile.copy(data = model.profile.data.copy(currentProfile = null)),
            welcome = model.welcome.copy(profileData = model.profile.data.copy(currentProfile = null)),
        )
        is CloseDialogRequest.OpenDirectory -> model.copy(
            closeDialog = null,
            profile = model.profile.copy(pendingPath = req.path),
        )
        null -> model.copy(closeDialog = null)
    }

    else -> model
}

private fun handleWelcomeMsg(msg: AppMsg.Welcome, model: AppModel): AppModel {
    val newWelcome = welcomeComponent.update(msg.msg, model.welcome)
    return when (msg.msg) {
        WelcomeMsg.ShowSettings   -> model.copy(welcome = newWelcome, showSettings = true)
        WelcomeMsg.ShowNewModpack -> model.copy(welcome = newWelcome, showNewModpack = true)
        is WelcomeMsg.DirectoryPicked ->
            handleDirectoryPicked(msg.msg.path, model).copy(welcome = newWelcome)
    }
}

private fun handleModpackMsg(msg: AppMsg.Modpack, model: AppModel): AppModel {
    val newModpack = modpackComponent.update(msg.msg, model.modpack)
    return when (msg.msg) {
        ModpackMsg.ShowSettings   -> model.copy(modpack = newModpack, showSettings = true)
        ModpackMsg.ShowNewModpack -> model.copy(modpack = newModpack, showNewModpack = true)

        is ModpackMsg.CloseRequested -> {
            if (model.modpack.actionName != null && !msg.msg.forceClose) {
                model.copy(modpack = newModpack, closeDialog = CloseDialogRequest.CloseModpack())
            } else {
                val clearedProfileData = model.profile.data.copy(currentProfile = null)
                model.copy(
                    modpack = ModpackModel(),
                    screen = AppScreen.Welcome,
                    profile = model.profile.copy(data = clearedProfileData),
                    welcome = model.welcome.copy(profileData = clearedProfileData),
                )
            }
        }

        is ModpackMsg.DirectoryPicked ->
            handleDirectoryPicked(msg.msg.path, model).copy(modpack = newModpack)

        // All other messages are fully handled by modpackUpdate — just commit the new child model
        else -> model.copy(modpack = newModpack)
    }
}

// ---------------------------------------------------------------------------
// appUpdate — pure fractal update
// ---------------------------------------------------------------------------

fun appUpdate(msg: AppMsg, model: AppModel): AppModel = when (msg) {

    // -- Profile --
    is AppMsg.ProfileLoaded,
    is AppMsg.ProfileCurrentResolved,
    is AppMsg.ThemeChangeRequested,
    is AppMsg.ThemeChanged           -> handleProfileMsg(msg, model)

    // -- Directory picker (app-level) --
    is AppMsg.DirectoryPicked        -> handleDirectoryPicked(msg.path, model)

    // -- Window --
    is AppMsg.WindowLoaded           -> model.copy(window = model.window.copy(data = msg.data, loaded = true))

    // -- Dialog dismissals --
    AppMsg.HideSettings              -> model.copy(showSettings = false)
    AppMsg.HideNewModpack            -> model.copy(showNewModpack = false)

    // -- Close dialog --
    is AppMsg.RequestCloseDialog,
    AppMsg.DismissCloseDialog,
    AppMsg.ConfirmCloseDialog        -> handleCloseDialogMsg(msg, model)

    // -- Quit --
    AppMsg.QuitReady                 -> model.copy(wantsQuit = false)

    // -- Pro / license --
    is AppMsg.ProActivationChecked   -> model.copy(isProActivated = msg.activated)

    is AppMsg.LicenseKeySubmit -> model.copy(pendingLicenseKey = msg.key, licenseKeyError = null)

    is AppMsg.LicenseKeyHandled -> model.copy(
        pendingLicenseKey = null,
        isProActivated = msg.activated ?: model.isProActivated,
        licenseKeyError = msg.error,
    )

    // -- Child components --
    is AppMsg.Welcome                -> handleWelcomeMsg(msg, model)
    is AppMsg.Modpack                -> handleModpackMsg(msg, model)
}

// ---------------------------------------------------------------------------
// appComponent — top-level component
// ---------------------------------------------------------------------------

val appComponent = component(
    init = AppModel(),
    update = ::appUpdate,
    view = { publish, model ->
        CloseDialog(publish, model)

        if (model.showSettings) {
            SettingsDialog(onDismiss = { publish(AppMsg.HideSettings) })
        }

        if (model.showNewModpack) {
            NewModpackDialog(
                profileData = model.profile.data,
                onDismiss = { publish(AppMsg.HideNewModpack) },
            )
        }

        val scope = LocalPakkuApplicationScope.current
        when (model.screen) {
            AppScreen.Welcome -> with(scope) {
                WelcomeView(
                    publish = { publish(AppMsg.Welcome(it)) },
                    model = model.welcome,
                    appModel = model,
                    appPublish = publish,
                )
            }
            AppScreen.Modpack -> with(scope) {
                ModpackView(
                    publish = { publish(AppMsg.Modpack(it)) },
                    model = model.modpack,
                    appModel = model,
                    appPublish = publish,
                )
            }
        }
    }
)