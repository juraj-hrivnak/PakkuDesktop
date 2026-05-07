/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import androidx.compose.runtime.Immutable
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.modpackComponent
import teksturepako.pakkuDesktop.app.ui.welcomeComponent
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

// ---------------------------------------------------------------------------
// Top-level app model
// ---------------------------------------------------------------------------

@Immutable
data class AppModel(
    val profile: ProfileModel = ProfileModel(),
    val window: WindowModel = WindowModel(),
    val screen: AppScreen = AppScreen.Welcome,
    // child component models — init mirrors childComponent.init()
    val welcome: WelcomeModel = welcomeComponent.init(),
    val modpack: ModpackModel = modpackComponent.init(),
    val showSettings: Boolean = false,
    val showNewModpack: Boolean = false,
    val closeDialog: CloseDialogRequest? = null,
    val isProActivated: Boolean? = null,
    val wantsQuit: Boolean = false,
)

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

sealed interface AppScreen {
    data object Welcome : AppScreen
    data object Modpack : AppScreen
}

// ---------------------------------------------------------------------------
// Welcome sub-model
// ---------------------------------------------------------------------------

@Immutable
data class WelcomeModel(
    val profileData: ProfileData = ProfileData(),
)

// ---------------------------------------------------------------------------
// Profile sub-model
// ---------------------------------------------------------------------------

data class ProfileModel(
    val data: ProfileData = ProfileData(),
    val loaded: Boolean = false,
    /** Non-null when the user wants to switch to a different directory. */
    val pendingPath: String? = null,
)

// ---------------------------------------------------------------------------
// Window sub-model
// ---------------------------------------------------------------------------

data class WindowModel(
    val data: WindowData = WindowData(),
    val loaded: Boolean = false,
)

// ---------------------------------------------------------------------------
// Close dialog
// ---------------------------------------------------------------------------

sealed interface CloseDialogRequest {
    val forceClose: Boolean

    data class CloseModpack(override val forceClose: Boolean = false) : CloseDialogRequest
    data class OpenDirectory(val path: String, override val forceClose: Boolean = false) : CloseDialogRequest
    data class Quit(override val forceClose: Boolean = true) : CloseDialogRequest
}

// ---------------------------------------------------------------------------
// Modpack sub-model
// ---------------------------------------------------------------------------

@Immutable
data class ModpackModel(
    val lockFile: Result<LockFile, ActionError>? = null,
    val configFile: Result<ConfigFile, ActionError>? = null,
    val loaded: Boolean = false,

    val selectedTab: SelectedTab = SelectedTab.PROJECTS,

    val selectedProject: Project? = null,
    val editingProject: Boolean = false,

    /** pakkuId → true for each selected project */
    val selectedPakkuIds: Set<String> = emptySet(),

    val sortOrder: SortOrder = SortOrder.Name(ascending = true),
    val projectsFilterText: String = "",

    /** Non-null while an action is running. */
    val actionName: String? = null,
    /** True when the user wants to terminate the running action. */
    val wantsTerminateAction: Boolean = false,
    /** True when an export has been requested by the view. */
    val wantsExport: Boolean = false,

    val toasts: List<ToastData> = emptyList(),

    /** Non-null while a property write is pending; cleared by projectEditDriver on completion. */
    val pendingPropertyWrite: PropertyWrite? = null,
)

// ---------------------------------------------------------------------------
// Supporting enums / sealed classes
// ---------------------------------------------------------------------------

enum class SelectedTab {
    MODPACK, PROJECTS, COMMIT
}

sealed class SortOrder(open val ascending: Boolean) {
    data class Name(override val ascending: Boolean) : SortOrder(ascending)
    data class LastUpdated(override val ascending: Boolean) : SortOrder(ascending)
}

