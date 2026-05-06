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
    // Toasts
    // -----------------------------------------------------------------------

    data class ToastAdded(val toast: ToastData) : ModpackMsg
    data class ToastDismissed(val id: String) : ModpackMsg
}


