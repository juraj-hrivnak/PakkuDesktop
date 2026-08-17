/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.data.toProjectsUiData
import teksturepako.pakkuDesktop.app.data.withProjectsUi
import teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg.WelcomeDropdown
import teksturepako.pakkuDesktop.app.ui.view.routes.ActivationView
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.app.ui.view.routes.WelcomeView
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.CloneRepositoryDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.ErrorDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.NewModpackDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog
import teksturepako.pakku.api.actions.errors.FileNotFound
import com.github.michaelbull.result.getError
import teksturepako.pakkuDesktop.elm.animatedRoute
import teksturepako.pakkuDesktop.elm.component

private fun AppModel.seededModpack(
    dropdown: ModpackDropdownModel = ModpackDropdownModel(profileData = profile.data),
): ModpackModel = ModpackModel(modpackDropdown = dropdown).withProjectsUi(projectsUi)

private fun AppModel.syncProjectsUiFromModpack(): AppModel {
    val data = modpack.toProjectsUiData()
    return if (data == projectsUi) this else copy(projectsUi = data)
}

private fun AppModel.commitModpack(newModpack: ModpackModel, persistListPrefs: Boolean): AppModel {
    val withModpack = copy(modpack = newModpack)
    return if (persistListPrefs) withModpack.syncProjectsUiFromModpack() else withModpack
}

private fun ModpackMsg.persistsProjectsListPrefs(): Boolean = when (this) {
    is ModpackMsg.SortOrderChanged,
    is ModpackMsg.FilterTextChanged,
    is ModpackMsg.FilterTypesChanged,
    is ModpackMsg.FilterSidesChanged,
    is ModpackMsg.FilterMissingSideChanged,
    is ModpackMsg.FilterProvidersChanged,
    is ModpackMsg.FilterRedistributableChanged,
    is ModpackMsg.ProjectsSplitRatioChanged -> true
    else -> false
}

// -- appUpdate handlers --

private fun AppModel.syncDropdownProfileData(data: teksturepako.pakkuDesktop.app.data.ProfileData): AppModel = copy(
    welcome = welcome.copy(
        profileData = data,
        dropdown = welcome.dropdown.copy(profileData = data),
    ),
    modpack = modpack.copy(
        modpackDropdown = modpack.modpackDropdown.copy(profileData = data),
    ),
)

private fun handleProfileMsg(msg: AppMsg, model: AppModel): AppModel = when (msg) {
    is AppMsg.ProfileLoaded -> {
        val newProfile = model.profile.copy(data = msg.data, loaded = true, pendingPath = null)
        val synced = model.copy(profile = newProfile).syncDropdownProfileData(msg.data)
        if (msg.data.currentProfile != null && model.screen == AppScreen.Welcome) {
            synced.copy(
                screen = AppScreen.Modpack,
                modpack = synced.seededModpack(ModpackDropdownModel(profileData = msg.data)),
            )
        } else synced
    }

    is AppMsg.ProfileCurrentResolved -> {
        val withProfile = model.copy(
            profile = model.profile.copy(data = msg.data, pendingPath = null),
            screen = if (msg.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome,
        ).syncDropdownProfileData(msg.data)
        withProfile.copy(
            modpack = withProfile.seededModpack(ModpackDropdownModel(profileData = msg.data)),
        )
    }

    is AppMsg.ThemeChangeRequested -> {
        val newData = model.profile.data.copy(theme = msg.theme.toString())
        model.copy(profile = model.profile.copy(data = newData)).syncDropdownProfileData(newData)
    }

    is AppMsg.ThemeChanged -> {
        model.copy(profile = model.profile.copy(data = msg.data)).syncDropdownProfileData(msg.data)
    }

    else -> model
}

private fun handleDirectoryPicked(path: String, model: AppModel): AppModel =
    if (model.modpack.actionName != null) {
        model.copy(closeDialog = CloseDialogRequest.OpenDirectory(path))
    } else {
        model.copy(profile = model.profile.copy(pendingPath = path))
    }

private fun applyCloseConfirm(model: AppModel, req: CloseDialogRequest): AppModel = when (req) {
    is CloseDialogRequest.Quit         -> model.copy(closeDialog = null, pendingCloseAfterTerminate = null, wantsQuit = true)
    is CloseDialogRequest.CloseModpack -> {
        val cleared = model.profile.data.copy(currentProfile = null)
        model.copy(
            closeDialog = null,
            pendingCloseAfterTerminate = null,
            screen = AppScreen.Welcome,
            modpack = model.seededModpack(ModpackDropdownModel()),
            profile = model.profile.copy(data = cleared),
            welcome = model.welcome.copy(
                profileData = cleared,
                dropdown = model.welcome.dropdown.copy(profileData = cleared),
            ),
        )
    }
    is CloseDialogRequest.OpenDirectory -> model.copy(
        closeDialog = null,
        pendingCloseAfterTerminate = null,
        profile = model.profile.copy(pendingPath = req.path),
    )
}

private fun handleCloseDialogMsg(msg: AppMsg, model: AppModel): AppModel = when (msg) {
    is AppMsg.RequestCloseDialog -> model.copy(closeDialog = msg.request)
    AppMsg.DismissCloseDialog    -> model.copy(closeDialog = null, pendingCloseAfterTerminate = null)

    AppMsg.ConfirmCloseDialog -> {
        val req = model.closeDialog ?: return model.copy(closeDialog = null)
        if (model.modpack.actionName != null) {
            // Terminate first; apply close after ActionFinished.
            model.copy(
                closeDialog = null,
                pendingCloseAfterTerminate = req,
                modpack = model.modpack.copy(wantsTerminateAction = true),
            )
        } else {
            applyCloseConfirm(model, req)
        }
    }

    else -> model
}

private fun AppModel.openSettings(): AppModel = copy(
    showSettings = true,
    wantsLoadCredentials = true,
    settingsCredentials = null,
    credentialsStatus = null,
)

private fun AppModel.closeSettings(): AppModel = copy(
    showSettings = false,
    wantsLoadCredentials = false,
    settingsCredentials = null,
    credentialsStatus = null,
    pendingCredentialsUpdate = null,
)

private fun handleWelcomeMsg(msg: AppMsg.Welcome, model: AppModel): AppModel {
    val newWelcome = welcomeComponent.update(msg.msg, model.welcome)
    return when (msg.msg) {
        WelcomeMsg.ShowSettings   -> model.copy(welcome = newWelcome).openSettings()
        WelcomeMsg.ShowNewModpack -> model.copy(welcome = newWelcome, showNewModpack = true)
        is WelcomeMsg.DirectoryPicked ->
            handleDirectoryPicked(msg.msg.path, model).copy(welcome = newWelcome)
        is WelcomeMsg.WelcomeDropdown -> {
            val base = model.copy(welcome = newWelcome)
            when (val inner = msg.msg.msg) {
                WelcomeDropdownMsg.NewModpack ->
                    base.copy(showNewModpack = true)
                WelcomeDropdownMsg.ShowSettings ->
                    base.openSettings()
                is WelcomeDropdownMsg.RecentProfile ->
                    handleDirectoryPicked(inner.path, base)
            }
        }
    }
}

private fun handleModpackMsg(msg: AppMsg.Modpack, model: AppModel): AppModel {
    val newModpack = modpackComponent.update(msg.msg, model.modpack)
    val persistListPrefs = msg.msg.persistsProjectsListPrefs()
    return when (msg.msg) {
        ModpackMsg.ShowSettings   -> model.commitModpack(newModpack, persistListPrefs).openSettings()
        ModpackMsg.ShowNewModpack -> model.commitModpack(newModpack, persistListPrefs).copy(showNewModpack = true)

        ModpackMsg.ActionFinished -> {
            val withModpack = model.commitModpack(newModpack, persistListPrefs)
            val pending = withModpack.pendingCloseAfterTerminate
            if (pending != null) {
                applyCloseConfirm(withModpack, pending)
            } else {
                withModpack
            }
        }

        is ModpackMsg.CloseRequested -> {
            if (model.modpack.actionName != null && !msg.msg.forceClose) {
                model.commitModpack(newModpack, persistListPrefs)
                    .copy(closeDialog = CloseDialogRequest.CloseModpack())
            } else {
                val clearedProfileData = model.profile.data.copy(currentProfile = null)
                model.copy(
                    modpack = model.seededModpack(ModpackDropdownModel()),
                    screen = AppScreen.Welcome,
                    profile = model.profile.copy(data = clearedProfileData),
                    welcome = model.welcome.copy(
                        profileData = clearedProfileData,
                        dropdown = model.welcome.dropdown.copy(profileData = clearedProfileData),
                    ),
                )
            }
        }

        is ModpackMsg.DirectoryPicked ->
            handleDirectoryPicked(msg.msg.path, model).commitModpack(newModpack, persistListPrefs)

        is ModpackMsg.FilesDropped -> {
            val withModpack = model.commitModpack(newModpack, persistListPrefs)
            val directories = msg.msg.paths.filter { java.nio.file.Files.isDirectory(java.nio.file.Path.of(it)) }
            val firstDir = directories.firstOrNull()
            if (firstDir != null) {
                handleDirectoryPicked(firstDir, withModpack)
            } else {
                // Treat dropped file names as add query hints (slug-like basenames).
                val query = msg.msg.paths
                    .map { java.nio.file.Path.of(it).fileName.toString().substringBeforeLast('.') }
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (query.isNotBlank()) {
                    withModpack.copy(modpack = newModpack.copy(pendingAddQuery = query))
                } else {
                    withModpack
                }
            }
        }

        // dropdown msgs that bubble here
        is ModpackMsg.ModpackDropdown -> when (val inner = msg.msg.msg) {
            is ModpackDropdownMsg.CloseRequested -> {
                if (model.modpack.actionName != null && !inner.force) {
                    model.commitModpack(newModpack, persistListPrefs)
                        .copy(closeDialog = CloseDialogRequest.CloseModpack())
                } else {
                    val cleared = model.profile.data.copy(currentProfile = null)
                    model.copy(
                        modpack = model.seededModpack(ModpackDropdownModel()),
                        screen = AppScreen.Welcome,
                        profile = model.profile.copy(data = cleared),
                        welcome = model.welcome.copy(
                            profileData = cleared,
                            dropdown = model.welcome.dropdown.copy(profileData = cleared),
                        ),
                    )
                }
            }
            is ModpackDropdownMsg.DirectoryPicked ->
                handleDirectoryPicked(inner.path, model).commitModpack(newModpack, persistListPrefs)
            ModpackDropdownMsg.ShowSettings ->
                model.commitModpack(newModpack, persistListPrefs).openSettings()
            else ->
                model.commitModpack(newModpack, persistListPrefs)
        }

        else -> model.commitModpack(newModpack, persistListPrefs)
    }
}

// -- appUpdate --

fun appUpdate(msg: AppMsg, model: AppModel): AppModel = when (msg) {

    // -- Profile --
    is AppMsg.ProfileLoaded,
    is AppMsg.ProfileCurrentResolved,
    is AppMsg.ThemeChangeRequested,
    is AppMsg.ThemeChanged           -> handleProfileMsg(msg, model)

    // -- Directory picker (app-level) --
    is AppMsg.DirectoryPicked        -> handleDirectoryPicked(msg.path, model)
    AppMsg.OpenDirectoryPickerRequested -> model.copy(wantsDirectoryPicker = true)
    AppMsg.DirectoryPickerLaunched   -> model.copy(wantsDirectoryPicker = false)

    // -- Window --
    is AppMsg.WindowLoaded           -> model.copy(window = model.window.copy(data = msg.data, loaded = true))

    // -- Dialog dismissals --
    AppMsg.HideSettings              -> model.closeSettings()
    AppMsg.HideNewModpack            -> model.copy(showNewModpack = false)
    AppMsg.ShowCloneDialog           -> model.copy(
        showCloneDialog = true,
        cloneStatus = null,
        cloneDestParent = null,
        wantsCloneParentPicker = false,
    )
    AppMsg.HideCloneDialog           -> model.copy(
        showCloneDialog = false,
        cloneStatus = null,
        pendingClone = null,
        cloneDestParent = null,
        wantsCloneParentPicker = false,
    )

    is AppMsg.CredentialsLoaded -> model.copy(
        wantsLoadCredentials = false,
        settingsCredentials = SettingsCredentials(
            curseForgeApiKey = msg.curseForgeApiKey,
            gitHubAccessToken = msg.gitHubAccessToken,
        ),
    )
    is AppMsg.CredentialsUpdateRequested -> model.copy(
        pendingCredentialsUpdate = CredentialsUpdateRequest(
            curseForgeApiKey = msg.curseForgeApiKey,
            gitHubAccessToken = msg.gitHubAccessToken,
        ),
        credentialsStatus = null,
    )
    is AppMsg.CredentialsUpdateHandled -> model.copy(
        pendingCredentialsUpdate = null,
        credentialsStatus = msg.statusMessage,
    )

    is AppMsg.CloneRequested -> model.copy(
        pendingClone = CloneRequest(url = msg.url, destPath = msg.destPath),
        cloneStatus = null,
    )
    is AppMsg.CloneFinished -> model.copy(
        pendingClone = null,
        cloneStatus = msg.errorMessage,
        showCloneDialog = if (msg.errorMessage == null) false else model.showCloneDialog,
        cloneDestParent = if (msg.errorMessage == null) null else model.cloneDestParent,
    )
    AppMsg.CloneParentPickerRequested -> model.copy(wantsCloneParentPicker = true)
    is AppMsg.CloneParentPicked -> model.copy(cloneDestParent = msg.path, wantsCloneParentPicker = false)
    AppMsg.CloneParentPickerLaunched -> model.copy(wantsCloneParentPicker = false)

    AppMsg.ShowActivation -> model.copy(screen = AppScreen.Activation, licenseKeyError = null)
    AppMsg.HideActivation -> model.copy(
        screen = if (model.profile.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome,
    )

    // -- Close dialog --
    is AppMsg.RequestCloseDialog,
    AppMsg.DismissCloseDialog,
    AppMsg.ConfirmCloseDialog        -> handleCloseDialogMsg(msg, model)

    // -- Quit --
    AppMsg.QuitReady                 -> model.copy(wantsQuit = false)

    // -- Pro / license --
    is AppMsg.ProActivationChecked   -> model.copy(isProActivated = msg.activated)

    is AppMsg.LicenseKeySubmit -> model.copy(pendingLicenseKey = msg.key, licenseKeyError = null)

    is AppMsg.LicenseKeyHandled -> {
        val activated = msg.activated ?: model.isProActivated
        model.copy(
            pendingLicenseKey = null,
            isProActivated = activated,
            licenseKeyError = msg.error,
            screen = if (activated == true && model.screen == AppScreen.Activation) {
                if (model.profile.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome
            } else {
                model.screen
            },
        )
    }

    // -- Child components --
    is AppMsg.Welcome                -> handleWelcomeMsg(msg, model)
    is AppMsg.Modpack                -> handleModpackMsg(msg, model)
}

// -- appComponent --

val appComponent = component(
    init = run {
        val projectsUi = ProjectsUiData.readOrNewBlocking()
        AppModel(
            projectsUi = projectsUi,
            modpack = ModpackModel().withProjectsUi(projectsUi),
        )
    },
    update = ::appUpdate,
    view = { publish, model ->
        CloseDialog(publish, model)

        val lockError = model.modpack.lockFile?.getError()?.takeUnless { it is FileNotFound }
        if (lockError != null && !model.modpack.lockErrorDismissed) {
            ErrorDialog(lockError) {
                publish(AppMsg.Modpack(ModpackMsg.DismissLockError))
            }
        }

        val configError = model.modpack.configFile?.getError()?.takeUnless { it is FileNotFound }
        if (configError != null && !model.modpack.configErrorDismissed) {
            ErrorDialog(configError) {
                publish(AppMsg.Modpack(ModpackMsg.DismissConfigError))
            }
        }

        if (model.showSettings) {
            SettingsDialog(
                model = model,
                publish = publish,
            )
        }

        if (model.showCloneDialog) {
            CloneRepositoryDialog(
                model = model,
                publish = publish,
            )
        }

        if (model.showNewModpack) {
            NewModpackDialog(
                profileData = model.profile.data,
                onDismiss = { publish(AppMsg.HideNewModpack) },
                publish = publish,
            )
        }

        val scope = LocalPakkuApplicationScope.current
        val routeKey = when (model.screen) {
            AppScreen.Modpack -> "modpack:${model.profile.data.currentProfile?.path}"
            AppScreen.Welcome -> "welcome"
            AppScreen.Activation -> "activation"
        }
        animatedRoute(routeKey) {
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
                AppScreen.Activation -> with(scope) {
                    ActivationView(
                        appPublish = publish,
                        isProActivated = model.isProActivated,
                        licenseKeyError = model.licenseKeyError,
                        intUiTheme = model.profile.data.intUiTheme,
                    )
                }
            }
        }
    }
)