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
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.actions.AdditionPlan
import teksturepako.pakkuDesktop.app.actions.RemovalPlan
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.modpackComponent
import teksturepako.pakkuDesktop.app.ui.welcomeComponent
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.pro.git.wrapper.GitEvent
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

// -- App model --

@Immutable
data class AppModel(
    val profile: ProfileModel = ProfileModel(),
    val window: WindowModel = WindowModel(),
    /** list prefs; synced with [modpack] UI state */
    val projectsUi: ProjectsUiData = ProjectsUiData(),
    val screen: AppScreen = AppScreen.Welcome,
    // child models — init mirrors childComponent.init()
    val welcome: WelcomeModel = welcomeComponent.init(),
    val modpack: ModpackModel = modpackComponent.init(),
    val showSettings: Boolean = false,
    val showNewModpack: Boolean = false,
    val showCloneDialog: Boolean = false,
    val closeDialog: CloseDialogRequest? = null,
    /**
     * When set, [ModpackMsg.TerminateAction] is in flight and this close request
     * will be applied after [ModpackMsg.ActionFinished].
     */
    val pendingCloseAfterTerminate: CloseDialogRequest? = null,
    val isProActivated: Boolean? = null,
    /** set on submit; cleared by licenseDriver */
    val pendingLicenseKey: String? = null,
    val licenseKeyError: ActionError? = null,
    val wantsQuit: Boolean = false,
    /** shortcut/menu asked to open the directory picker */
    val wantsDirectoryPicker: Boolean = false,
    /** settings dialog: load ~/.pakku/credentials; cleared by credentialsDriver */
    val wantsLoadCredentials: Boolean = false,
    /** settings dialog form seed; null while loading */
    val settingsCredentials: SettingsCredentials? = null,
    /** pending write; cleared by credentialsDriver */
    val pendingCredentialsUpdate: CredentialsUpdateRequest? = null,
    /** settings save status text */
    val credentialsStatus: String? = null,
    /** clone in flight */
    val pendingClone: CloneRequest? = null,
    /** clone parent directory (Browse…); set by directoryPickerDriver */
    val cloneDestParent: String? = null,
    /** clone dialog: open parent-directory picker */
    val wantsCloneParentPicker: Boolean = false,
    /** clone error/status; null when idle */
    val cloneStatus: String? = null,
)

data class SettingsCredentials(
    val curseForgeApiKey: String,
    val gitHubAccessToken: String,
)

data class CredentialsUpdateRequest(
    val curseForgeApiKey: String,
    val gitHubAccessToken: String,
)

data class CloneRequest(
    val url: String,
    val destPath: String,
)
// -- Navigation --

sealed interface AppScreen {
    data object Welcome : AppScreen
    data object Modpack : AppScreen
    data object Activation : AppScreen
}

// -- Welcome sub-model --

@Immutable
data class WelcomeModel(
    val profileData: ProfileData = ProfileData(),
    val dropdown: WelcomeDropdownModel = WelcomeDropdownModel(),
)

// -- Profile sub-model --

data class ProfileModel(
    val data: ProfileData = ProfileData(),
    val loaded: Boolean = false,
    /** directory switch request */
    val pendingPath: String? = null,
)

// -- Window sub-model --

data class WindowModel(
    val data: WindowData = WindowData(),
    val loaded: Boolean = false,
)

// -- Close dialog --

sealed interface CloseDialogRequest {
    val forceClose: Boolean

    data class CloseModpack(override val forceClose: Boolean = false) : CloseDialogRequest
    data class OpenDirectory(val path: String, override val forceClose: Boolean = false) : CloseDialogRequest
    data class Quit(override val forceClose: Boolean = true) : CloseDialogRequest
}

// -- Modpack sub-model --

@Immutable
data class ModpackModel(
    val lockFile: Result<LockFile, ActionError>? = null,
    val configFile: Result<ConfigFile, ActionError>? = null,
    val loaded: Boolean = false,

    val selectedTab: SelectedTab = SelectedTab.PROJECTS,

    val selectedProject: Project? = null,
    val editingProject: Boolean = false,
    /** True while the Modpack tab is in edit mode (auto-saves like project props). */
    val editingModpack: Boolean = false,

    /** pakkuId → true for each selected project */
    val selectedPakkuIds: Set<String> = emptySet(),

    val sortOrder: SortOrder = SortOrder.Name(ascending = true),
    val projectsFilterText: String = "",
    /** Empty = no type filter; otherwise project type must be in the set. */
    val filterTypes: Set<ProjectType> = emptySet(),
    /** Empty = no side filter; otherwise project side must be in the set. */
    val filterSides: Set<ProjectSide> = emptySet(),
    /** Empty = no provider filter; values are provider [serialName]s. */
    val filterProviders: Set<String> = emptySet(),
    /** First-pane weight for the Projects list|inspector split (list share). */
    val projectsSplitRatio: Float = ProjectsUiData.DEFAULT_SPLIT_RATIO,
    /** only projects with a pending update from the last status check */
    val filterUpdatesOnly: Boolean = false,
    val wantsStatusCheck: Boolean = false,
    /**
     * Last status check: [Project.pakkuId] → update info.
     * null = not checked; empty = all up to date.
     */
    val updatePreviews: Map<String, ProjectUpdateInfo>? = null,

    val actionName: String? = null,
    val wantsTerminateAction: Boolean = false,
    val wantsExport: Boolean = false,
    val wantsFetch: Boolean = false,
    val wantsUpdate: Boolean = false,
    val wantsFocusProjectsFilter: Boolean = false,
    val projectsFilterFocused: Boolean = false,
    /** Add projects dialog (Projects list FAB) */
    val addDialogVisible: Boolean = false,
    /** Remove projects dialog (FAB / Delete) */
    val removeDialogVisible: Boolean = false,
    /** DnD / auto add query */
    val pendingAddQuery: String? = null,
    val pendingAdditionPlan: AdditionPlan? = null,
    /** selected ids + recommended orphaned deps */
    val pendingRemovalIds: Set<String>? = null,
    val pendingRemovalPlan: RemovalPlan? = null,
    val wantsInit: Boolean = false,

    /** cleared on next [ModpackMsg.Loaded] */
    val lockErrorDismissed: Boolean = false,
    /** cleared on next [ModpackMsg.Loaded] */
    val configErrorDismissed: Boolean = false,
    /** cleared by projectMutationDriver */
    val pendingInitSpec: InitSpec? = null,

    val toasts: List<ToastData> = emptyList(),

    /** cleared by projectEditDriver when done */
    val pendingPropertyWrite: PropertyWrite? = null,
    val pendingMetaWrite: MetaWrite? = null,

    // -- Git (Pro) --
    val git: GitState = GitState(),
    val gitCurrentDiff: DiffContent? = null,
    val gitDiffPendingFile: GitFile? = null,
    val gitEventProgress: GitEvent.Progress? = null,
    val wantsGitPull: Boolean = false,
    val wantsGitPush: Boolean = false,
    val wantsGitCommit: Boolean = false,
    val gitCheckoutBranch: GitBranch? = null,

    // -- Dropdown child models --
    val modpackDropdown: ModpackDropdownModel = ModpackDropdownModel(),
    val gitDropdown: GitDropdownModel = GitDropdownModel(),
)

// -- Enums / sealed --

enum class SelectedTab {
    MODPACK, PROJECTS, COMMIT
}

sealed class SortOrder(open val ascending: Boolean) {
    data class Name(override val ascending: Boolean) : SortOrder(ascending)
    data class LastUpdated(override val ascending: Boolean) : SortOrder(ascending)
}

