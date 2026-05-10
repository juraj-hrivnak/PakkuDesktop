/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
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
    data class GitCommitMessageChanged(val message: String) : ModpackMsg
    data class GitDiffFileSelected(val file: GitFile) : ModpackMsg
    data class GitDiffComputed(val diff: DiffContent?) : ModpackMsg
    data class GitEventProgressUpdated(val progress: GitEvent.Progress?) : ModpackMsg

    data object GitPullRequested : ModpackMsg
    data object GitPullFinished : ModpackMsg
    data object GitPushRequested : ModpackMsg
    data object GitPushFinished : ModpackMsg
    data object GitCommitRequested : ModpackMsg
    data object GitCommitFinished : ModpackMsg
    data class GitCheckoutRequested(val branch: GitBranch) : ModpackMsg
    data object GitCheckoutFinished : ModpackMsg
}


