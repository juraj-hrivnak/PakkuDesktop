/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.plugins.PluginRegistry
import teksturepako.pakkuDesktop.app.plugins.pluginsRoot as defaultPluginsRoot
import teksturepako.pakkuDesktop.app.data.toProjectsUiData
import teksturepako.pakkuDesktop.app.data.withHistoryFrom
import teksturepako.pakkuDesktop.app.data.withProjectsUi
import teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog
import teksturepako.pakkuDesktop.app.ui.model.*
import teksturepako.pakkuDesktop.app.ui.view.plugins.PakkuProSlot
import teksturepako.pakkuDesktop.app.ui.view.routes.ActivationView
import teksturepako.pakkuDesktop.app.ui.view.routes.AppFrame
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.CloneRepositoryDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.ErrorDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.NewModpackDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog
import teksturepako.pakku.api.actions.errors.FileNotFound
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.withChild
import java.nio.file.Path

private fun AppModel.seededModpack(
    dropdown: ModpackDropdownModel = ModpackDropdownModel(profileData = profile.data),
): ModpackModel = ModpackModel(modpackDropdown = dropdown).withProjectsUi(projectsUi)

private fun AppModel.syncProjectsUiFromModpack(): AppModel {
    val data = modpack.toProjectsUiData()
    return if (data == projectsUi) this else copy(projectsUi = data)
}

private fun AppModel.commitModpack(newModpack: ModpackModel, persistListPrefs: Boolean): AppModel =
    withChild(modpack, newModpack) {
        val withModpack = copy(modpack = it)
        if (persistListPrefs) withModpack.syncProjectsUiFromModpack() else withModpack
    }

private fun ModpackMsg.persistsProjectsListPrefs(): Boolean = when (this) {
    is ModpackMsg.SortOrderChanged,
    is ModpackMsg.FilterTextChanged,
    is ModpackMsg.FilterTypesChanged,
    is ModpackMsg.FilterSidesChanged,
    is ModpackMsg.FilterMissingSideChanged,
    is ModpackMsg.FilterProvidersChanged,
    is ModpackMsg.FilterRedistributableChanged,
    is ModpackMsg.ProjectsSplitRatioChanged,
    is ModpackMsg.FilesSplitRatioChanged,
    is ModpackMsg.GitGroupUnversionedChanged -> true
    else -> false
}

// -- appUpdate handlers --

private fun AppModel.syncDropdownProfileData(data: ProfileData): AppModel {
    val dropdown = welcome.dropdown.withChild(welcome.dropdown.profileData, data) { copy(profileData = it) }
    val newWelcome = welcome
        .withChild(welcome.profileData, data) { copy(profileData = it) }
        .withChild(welcome.dropdown, dropdown) { copy(dropdown = it) }
    val modpackDropdown = modpack.modpackDropdown
        .withChild(modpack.modpackDropdown.profileData, data) { copy(profileData = it) }
    return withChild(welcome, newWelcome) { copy(welcome = it) }
        .withChild(modpack.modpackDropdown, modpackDropdown) { copy(modpack = modpack.copy(modpackDropdown = it)) }
}

private fun handleProfileMsg(msg: AppMsg, model: AppModel): AppModel = when (msg) {
    is AppMsg.ProfileLoaded -> {
        val data = msg.data.copy(uiScale = ProfileData.coerceUiScale(msg.data.uiScale))
        val newProfile = model.profile.copy(data = data, loaded = true, pendingPath = null)
        val synced = model.copy(profile = newProfile).syncDropdownProfileData(data)
        if (data.currentProfile != null && model.screen == AppScreen.Welcome) {
            synced.copy(
                screen = AppScreen.Modpack,
                modpack = synced.seededModpack(ModpackDropdownModel(profileData = data)),
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

    is AppMsg.UiScaleChangeRequested -> {
        val newData = model.profile.data.copy(
            uiScale = ProfileData.coerceUiScale(msg.uiScale),
        )
        model.copy(profile = model.profile.copy(data = newData)).syncDropdownProfileData(newData)
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
    is AppMsg.RequestCloseDialog -> {
        val req = msg.request
        if (req is CloseDialogRequest.Quit && model.screen == AppScreen.Welcome) {
            if (model.modpack.actionName != null) {
                model.copy(closeDialog = req)
            } else {
                applyCloseConfirm(model, req)
            }
        } else {
            model.copy(closeDialog = req)
        }
    }
    AppMsg.DismissCloseDialog    -> model.copy(closeDialog = null, pendingCloseAfterTerminate = null)

    AppMsg.ConfirmCloseDialog -> {
        val req = model.closeDialog ?: return model
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
    settingsTab = SettingsTab.General,
    wantsLoadCredentials = true,
    internal = internal.copy(settingsCredentials = null),
    credentialsStatus = null,
    wantsHeapHistogram = false,
)

private fun AppModel.closeSettings(): AppModel = copy(
    showSettings = false,
    settingsTab = SettingsTab.General,
    wantsLoadCredentials = false,
    internal = InternalModel(),
    credentialsStatus = null,
    wantsHeapHistogram = false,
)

private fun handleWelcomeMsg(msg: AppMsg.Welcome, model: AppModel): AppModel {
    val newWelcome = welcomeComponent.update(msg.msg, model.welcome)
    val base = model.withChild(model.welcome, newWelcome) { copy(welcome = it) }
    return when (val child = msg.msg) {
        WelcomeMsg.ShowSettings   -> base.openSettings()
        WelcomeMsg.ShowNewModpack -> base.copy(showNewModpack = true)
        is WelcomeMsg.DirectoryPicked ->
            handleDirectoryPicked(child.path, base)
        is WelcomeMsg.WelcomeDropdown -> {
            when (val inner = child.msg) {
                WelcomeDropdownMsg.NewModpack ->
                    base.copy(showNewModpack = true)
                WelcomeDropdownMsg.ShowSettings ->
                    base.openSettings()
                is WelcomeDropdownMsg.RecentProfile ->
                    handleDirectoryPicked(inner.path, base)
            }
        }
        is WelcomeMsg.SummariesLoaded -> base
    }
}

private fun handleModpackMsg(msg: AppMsg.Modpack, model: AppModel): AppModel {
    val newModpack = modpackComponent.update(msg.msg, model.modpack)
    val persistListPrefs = msg.msg.persistsProjectsListPrefs()
    val updated = when (val child = msg.msg) {
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
            if (model.modpack.actionName != null && !child.forceClose) {
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
            handleDirectoryPicked(child.path, model).commitModpack(newModpack, persistListPrefs)

        is ModpackMsg.FilesDropped -> {
            val withModpack = model.commitModpack(newModpack, persistListPrefs)
            val directories = child.paths.filter { java.nio.file.Files.isDirectory(java.nio.file.Path.of(it)) }
            val firstDir = directories.firstOrNull()
            if (firstDir != null) {
                handleDirectoryPicked(firstDir, withModpack)
            } else {
                // Treat dropped file names as add query hints (slug-like basenames).
                val query = child.paths
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
        is ModpackMsg.ModpackDropdown -> when (val inner = child.msg) {
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
    return when (msg.msg) {
        is ModpackMsg.MetaWriteRequested,
        is ModpackMsg.Loaded,
        is ModpackMsg.MetaWriteFailed -> updated.reflectModpackName()
        else -> updated
    }
}

/** Keeps the dropdown, window title, and recent entry on the name stored in `pakku.json`. */
private fun AppModel.reflectModpackName(): AppModel {
    val name = modpack.configFile?.get()?.getName()?.trim()?.takeIf { it.isNotEmpty() } ?: return this
    val current = profile.data.currentProfile ?: return this
    val named = if (current.name == name) {
        this
    } else {
        val data = profile.data.withCurrentProfile(current.copy(name = name))
        copy(profile = profile.copy(data = data)).syncDropdownProfileData(data)
    }
    val summary = named.welcome.summaries[current.path] ?: return named
    if (summary.name == name) return named
    return named.copy(
        welcome = named.welcome.copy(
            summaries = named.welcome.summaries.put(current.path, summary.copy(name = name)),
        ),
    )
}

// -- appUpdate --

fun appUpdate(msg: AppMsg, model: AppModel, plugins: PluginRegistry = PluginRegistry.Empty): AppModel = when (msg) {

    // -- Profile --
    is AppMsg.ProfileLoaded,
    is AppMsg.ProfileCurrentResolved,
    is AppMsg.ThemeChangeRequested,
    is AppMsg.ThemeChanged,
    is AppMsg.UiScaleChangeRequested -> handleProfileMsg(msg, model)

    // -- Directory picker (app-level) --
    is AppMsg.DirectoryPicked        -> handleDirectoryPicked(msg.path, model)
    AppMsg.OpenDirectoryPickerRequested -> model.copy(wantsDirectoryPicker = true)
    AppMsg.DirectoryPickerLaunched   -> model.copy(wantsDirectoryPicker = false)

    // -- Window --
    is AppMsg.WindowLoaded           -> model.copy(window = model.window.copy(data = msg.data, loaded = true))

    // -- Dialog dismissals --
    AppMsg.HideSettings              -> model.closeSettings()
    is AppMsg.SettingsTabSelected    ->
        if (model.settingsTab == msg.tab) model else model.copy(settingsTab = msg.tab)
    AppMsg.HeapHistogramRequested    ->
        if (model.wantsHeapHistogram) model else model.copy(wantsHeapHistogram = true)
    is AppMsg.HeapHistogramLoaded    -> model.copy(
        wantsHeapHistogram = false,
        heapHistogram = msg.histogram,
    )
    is AppMsg.HeapFootprintSampled   -> {
        val prev = model.heapFootprint
        val next = msg.footprint
        val lastAction = prev?.history?.lastOrNull()?.action
        if (
            prev != null &&
            prev.rssBytes == next.rssBytes &&
            prev.heapUsedBytes == next.heapUsedBytes &&
            lastAction == msg.action
        ) {
            model
        } else {
            model.copy(heapFootprint = next.withHistoryFrom(prev, msg.action))
        }
    }
    AppMsg.HideNewModpack            ->
        if (!model.showNewModpack) model else model.copy(showNewModpack = false)
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

    is AppMsg.CredentialsUpdateHandled -> model.copy(
        internal = model.internal.copy(pendingCredentialsUpdate = null),
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

    AppMsg.ShowActivation -> model.copy(screen = AppScreen.Activation)
    AppMsg.HideActivation -> model.copy(
        screen = if (model.profile.data.currentProfile != null) AppScreen.Modpack else AppScreen.Welcome,
    )

    // -- Close dialog --
    is AppMsg.RequestCloseDialog,
    AppMsg.DismissCloseDialog,
    AppMsg.ConfirmCloseDialog        -> handleCloseDialogMsg(msg, model)

    // -- Quit --
    AppMsg.QuitReady                 -> model.copy(wantsQuit = false)

    // -- Internal (credentials, license) --
    is AppMsg.Internal               -> handleInternalMsg(msg.msg, model)

    // -- Child components --
    is AppMsg.Welcome                -> handleWelcomeMsg(msg, model)
    is AppMsg.Modpack                -> handleModpackMsg(msg, model)
    is AppMsg.Plugin                 -> handlePluginMsg(msg, model, plugins)
}

private fun handleInternalMsg(msg: InternalMsg, model: AppModel): AppModel = when (msg) {
    is InternalMsg.CredentialsLoaded -> model.copy(
        wantsLoadCredentials = false,
        internal = model.internal.copy(
            settingsCredentials = SettingsCredentials(
                curseForgeApiKey = msg.curseForgeApiKey,
                gitHubAccessToken = msg.gitHubAccessToken,
            ),
        ),
    )
    is InternalMsg.CredentialsUpdateRequested -> model.copy(
        internal = model.internal.copy(
            pendingCredentialsUpdate = CredentialsUpdateRequest(
                curseForgeApiKey = msg.curseForgeApiKey,
                gitHubAccessToken = msg.gitHubAccessToken,
            ),
        ),
        credentialsStatus = null,
    )
    is InternalMsg.ProActivationReported ->
        if (model.isProActivated == msg.activated) model
        else model.copy(isProActivated = msg.activated)
}

private fun handlePluginMsg(msg: AppMsg.Plugin, model: AppModel, plugins: PluginRegistry): AppModel {
    val plugin = plugins.loaded[msg.id] ?: return model
    val old = model.pluginStates[msg.id]
    val new = plugin.erased.update(msg.msg, old)
    return if (new === old) model else model.copy(pluginStates = model.pluginStates.put(msg.id, new))
}

// -- appComponent --

fun appComponent(
    initialProjectsUi: ProjectsUiData,
    plugins: PluginRegistry = PluginRegistry.Empty,
    pluginsRoot: Path = defaultPluginsRoot(),
) = component(
    init = AppModel(
        projectsUi = initialProjectsUi,
        modpack = ModpackModel().withProjectsUi(initialProjectsUi),
        pluginStates = plugins.loaded.mapValues { it.value.initialState }.toPersistentMap(),
        pluginFailures = plugins.failed.toPersistentList(),
    ),
    update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
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
                plugins = plugins,
                pluginsRoot = pluginsRoot,
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
        with(scope) {
            AppFrame(
                model = model,
                publish = publish,
                activation = {
                    ActivationView(appPublish = publish) {
                        PakkuProSlot(
                            plugins = plugins,
                            states = model.pluginStates,
                            failures = model.pluginFailures,
                            publish = publish,
                        )
                    }
                },
            )
        }
    }
)