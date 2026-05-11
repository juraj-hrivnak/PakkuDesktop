/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.pro.git.GitEvent
import teksturepako.pakkuDesktop.pro.git.GitState
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

/** Wraps a pending project property write so the driver can fulfil it as a side effect. */
data class PropertyWrite(
    val write: ConfigFile.ProjectConfig.(slug: String) -> Unit,
)

// ---------------------------------------------------------------------------
// ModpackDropdown child model & messages
// ---------------------------------------------------------------------------

data class ModpackDropdownModel(
    val profileData: ProfileData = ProfileData(),
    val actionEnabled: Boolean = true,
    val enabled: Boolean = true,
)

sealed interface ModpackDropdownMsg {
    // Cross-cutting — parent handles, child returns model unchanged
    data class CloseRequested(val force: Boolean = false) : ModpackDropdownMsg
    data object Export : ModpackDropdownMsg
    data class DirectoryPicked(val path: String) : ModpackDropdownMsg
}

// ---------------------------------------------------------------------------
// GitDropdown child model & messages
// ---------------------------------------------------------------------------

data class GitDropdownModel(
    val gitState: GitState = GitState(),
    val pushDialogVisible: Boolean = false,
)

sealed interface GitDropdownMsg {
    data object ShowPushDialog : GitDropdownMsg
    data object HidePushDialog : GitDropdownMsg
    // Cross-cutting — parent handles, child returns model unchanged
    data object PullRequested : GitDropdownMsg
    data object PushRequested : GitDropdownMsg
    data class TabSelected(val tab: SelectedTab) : GitDropdownMsg
    data class CheckoutRequested(val branch: GitBranch) : GitDropdownMsg
}

// ---------------------------------------------------------------------------
// ModpackMsg
// ---------------------------------------------------------------------------

sealed interface ModpackMsg {

    // -----------------------------------------------------------------------
    // Driver messages
    // -----------------------------------------------------------------------

    data class Loaded(
        val lockFile: Result<LockFile, ActionError>,
        val configFile: Result<ConfigFile, ActionError>,
    ) : ModpackMsg

    data object Reset : ModpackMsg

    // -----------------------------------------------------------------------
    // Cross-cutting — parent handles these, child update returns model unchanged
    // -----------------------------------------------------------------------

    /** Parent maps → model.showSettings = true */
    data object ShowSettings : ModpackMsg

    /** Parent maps → model.showNewModpack = true */
    data object ShowNewModpack : ModpackMsg

    /**
     * Parent maps → navigate to Welcome (or show close dialog if action is running).
     * [forceClose] skips the dialog.
     */
    data class CloseRequested(val forceClose: Boolean = false) : ModpackMsg

    /** Parent maps → profile.pendingPath (or shows close dialog if action is running). */
    data class DirectoryPicked(val path: String) : ModpackMsg

    // -----------------------------------------------------------------------
    // Tab
    // -----------------------------------------------------------------------

    data class TabSelected(val tab: SelectedTab) : ModpackMsg

    // -----------------------------------------------------------------------
    // Projects
    // -----------------------------------------------------------------------

    data class ProjectSelected(val project: Project?) : ModpackMsg
    data class ProjectEditing(val editing: Boolean) : ModpackMsg

    data class ProjectsSelected(val pakkuIds: Set<String>) : ModpackMsg
    data class ProjectsDeselected(val pakkuIds: Set<String>) : ModpackMsg
    data class ProjectsCleared(val dummy: Unit = Unit) : ModpackMsg

    // -----------------------------------------------------------------------
    // Sort / filter
    // -----------------------------------------------------------------------

    data class SortOrderChanged(val order: SortOrder) : ModpackMsg
    data class FilterTextChanged(val text: String) : ModpackMsg

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    data object ExportRequested : ModpackMsg
    data class ActionStarted(val name: String) : ModpackMsg
    data object ActionFinished : ModpackMsg
    data object TerminateAction : ModpackMsg

    // -----------------------------------------------------------------------
    // Project property editing — fulfilled by projectEditDriver
    // -----------------------------------------------------------------------

    /** View dispatches this; projectEditDriver writes to disk then publishes Loaded. */
    data class PropertyWriteRequested(val request: PropertyWrite) : ModpackMsg
    /** projectEditDriver dispatches this after the write + reload are complete. */
    data object PropertyWriteCompleted : ModpackMsg

    // -----------------------------------------------------------------------
    // Toasts
    // -----------------------------------------------------------------------

    data class ToastAdded(val toast: ToastData) : ModpackMsg
    data class ToastDismissed(val id: String) : ModpackMsg

    // -----------------------------------------------------------------------
    // Git (Pro) — intents & driver completions
    // -----------------------------------------------------------------------

    data class GitStateUpdated(val state: GitState) : ModpackMsg
    data class GitFileSelectionToggled(val file: GitFile) : ModpackMsg
    /** Toggle inclusion for every changed file under [folderPath] (path prefix, `/`-separated). */
    data class GitFolderSelectionToggled(val folderPath: String) : ModpackMsg
    /** Expand/collapse a folder row in the changelist tree ([GitState.expandedFolderPaths]). */
    data class GitChangelistFolderExpansionToggled(val folderPath: String) : ModpackMsg
    data object GitChangelistExpandAllFolders : ModpackMsg
    data object GitChangelistCollapseAllFolders : ModpackMsg
    /** Select every changed file for the next commit (working tree → index via `git add` on commit). */
    data object GitSelectAllChangedFiles : ModpackMsg
    /** Clear the inclusion set; nothing is staged until the user checks files again. */
    data object GitClearChangedFileSelection : ModpackMsg
    data class GitCommitMessageChanged(val message: String) : ModpackMsg
    data class GitDiffFileSelected(val file: GitFile) : ModpackMsg
    data class GitDiffComputed(val diff: DiffContent?) : ModpackMsg
    data class GitEventProgressUpdated(val progress: GitEvent.Progress?) : ModpackMsg

    data object GitPullRequested : ModpackMsg
    data object GitPullFinished : ModpackMsg
    data object GitPushRequested : ModpackMsg
    data object GitPushFinished : ModpackMsg
    data object GitCommitRequested : ModpackMsg
    /** Published by gitDriver after [GitCommitRequested]; [success] is false if the commit command failed. */
    data class GitCommitFinished(val success: Boolean) : ModpackMsg
    data class GitCheckoutRequested(val branch: GitBranch) : ModpackMsg
    data object GitCheckoutFinished : ModpackMsg

    // -----------------------------------------------------------------------
    // Dropdown child messages (fractal delegation)
    // -----------------------------------------------------------------------

    /** Fractal delegation — wraps all ModpackDropdown child messages. */
    data class ModpackDropdown(val msg: ModpackDropdownMsg) : ModpackMsg
    /** Fractal delegation — wraps all GitDropdown child messages. */
    data class GitDropdown(val msg: GitDropdownMsg) : ModpackMsg
}
