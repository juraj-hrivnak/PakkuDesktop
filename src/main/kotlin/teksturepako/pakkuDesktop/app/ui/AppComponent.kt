/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.*
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.NewModpackDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// Composition Locals
// ---------------------------------------------------------------------------

val LocalAppPublish = compositionLocalOf<(AppMsg) -> Unit> { {} }
val LocalAppModel   = compositionLocalOf<AppModel> { AppModel() }
val LocalPakkuApplicationScope = compositionLocalOf<PakkuApplicationScope> {
    error("No PakkuApplicationScope provided")
}

// ---------------------------------------------------------------------------
// appUpdate — pure fractal update
// ---------------------------------------------------------------------------

fun appUpdate(msg: AppMsg, model: AppModel): AppModel = when (msg) {

    // -- Profile (driver callbacks) --

    is AppMsg.ProfileLoaded -> {
        val newProfile = model.profile.copy(data = msg.data, loaded = true, pendingPath = null)
        val syncedWelcome = model.welcome.copy(profileData = msg.data)
        val base = model.copy(profile = newProfile, welcome = syncedWelcome)
        if (msg.data.currentProfile != null && model.screen == AppScreen.Welcome) {
            base.copy(screen = AppScreen.Modpack, modpack = ModpackModel())
        } else base
    }

    is AppMsg.DirectoryPicked -> {
        // From OS file-picker driver (app-level)
        if (model.modpack.actionName != null) {
            model.copy(closeDialog = CloseDialogRequest.OpenDirectory(msg.path))
        } else {
            model.copy(profile = model.profile.copy(pendingPath = msg.path))
        }
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

    // -- Window --

    is AppMsg.WindowLoaded -> model.copy(window = model.window.copy(data = msg.data, loaded = true))

    // -- Dialog dismissals (from AppView lambdas) --

    AppMsg.HideSettings  -> model.copy(showSettings = false)
    AppMsg.HideNewModpack -> model.copy(showNewModpack = false)

    // -- Close / Quit dialog --

    is AppMsg.RequestCloseDialog -> model.copy(closeDialog = msg.request)
    AppMsg.DismissCloseDialog    -> model.copy(closeDialog = null)

    AppMsg.ConfirmCloseDialog -> when (val req = model.closeDialog) {
        is CloseDialogRequest.Quit -> model.copy(closeDialog = null, wantsQuit = true)
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

    AppMsg.QuitReady -> model.copy(wantsQuit = false)

    // -- Pro --

    is AppMsg.ProActivationChecked -> model.copy(isProActivated = msg.activated)

    // -----------------------------------------------------------------------
    // Fractal child delegation
    // -----------------------------------------------------------------------

    is AppMsg.Welcome -> {
        // 1. delegate to child update
        val newWelcome = welcomeComponent.update(msg.msg, model.welcome)
        // 2. parent handles cross-cutting effects
        when (msg.msg) {
            WelcomeMsg.ShowSettings   -> model.copy(welcome = newWelcome, showSettings = true)
            WelcomeMsg.ShowNewModpack -> model.copy(welcome = newWelcome, showNewModpack = true)
            is WelcomeMsg.DirectoryPicked -> {
                if (model.modpack.actionName != null) {
                    model.copy(welcome = newWelcome, closeDialog = CloseDialogRequest.OpenDirectory(msg.msg.path))
                } else {
                    model.copy(welcome = newWelcome, profile = model.profile.copy(pendingPath = msg.msg.path))
                }
            }
        }
    }

    is AppMsg.Modpack -> {
        // 1. delegate to child update
        val newModpack = modpackComponent.update(msg.msg, model.modpack)
        // 2. parent handles cross-cutting effects
        when (msg.msg) {
            ModpackMsg.ShowSettings -> model.copy(modpack = newModpack, showSettings = true)
            ModpackMsg.ShowNewModpack -> model.copy(modpack = newModpack, showNewModpack = true)

            is ModpackMsg.CloseRequested -> {
                if (model.modpack.actionName != null && !msg.msg.forceClose) {
                    // action running — ask
                    model.copy(modpack = newModpack, closeDialog = CloseDialogRequest.CloseModpack())
                } else {
                    // navigate away
                    val clearedProfileData = model.profile.data.copy(currentProfile = null)
                    model.copy(
                        modpack = ModpackModel(),
                        screen = AppScreen.Welcome,
                        profile = model.profile.copy(data = clearedProfileData),
                        welcome = model.welcome.copy(profileData = clearedProfileData),
                    )
                }
            }

            is ModpackMsg.DirectoryPicked -> {
                if (model.modpack.actionName != null) {
                    model.copy(modpack = newModpack, closeDialog = CloseDialogRequest.OpenDirectory(msg.msg.path))
                } else {
                    model.copy(modpack = newModpack, profile = model.profile.copy(pendingPath = msg.msg.path))
                }
            }

            else -> model.copy(modpack = newModpack)
        }
    }
}

// ---------------------------------------------------------------------------
// appComponent — top-level component
// ---------------------------------------------------------------------------

val appComponent = component(
    init = AppModel(),
    update = ::appUpdate,
    view = { publish, model ->
        CompositionLocalProvider(
            LocalAppPublish provides publish,
            LocalAppModel provides model,
        ) {
            CloseDialog(publish, model)

            if (model.showSettings) {
                SettingsDialog(onDismiss = { publish(AppMsg.HideSettings) })
            }

            if (model.showNewModpack) {
                NewModpackDialog(
                    profileData = model.profile.data,
                    onDismiss = { publish(AppMsg.HideNewModpack) }
                )
            }

            when (model.screen) {
                AppScreen.Welcome -> welcomeComponent.view({ publish(AppMsg.Welcome(it)) }, model.welcome)
                AppScreen.Modpack -> modpackComponent.view({ publish(AppMsg.Modpack(it)) }, model.modpack)
            }
        }
    }
)
