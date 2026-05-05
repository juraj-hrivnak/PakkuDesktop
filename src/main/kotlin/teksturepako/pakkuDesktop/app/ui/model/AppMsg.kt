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
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

sealed interface AppMsg {

    // -----------------------------------------------------------------------
    // Profile
    // -----------------------------------------------------------------------

    /** Driver publishes this after loading from disk. */
    data class ProfileLoaded(val data: ProfileData) : AppMsg

    /** View publishes this when user picks a directory. */
    data class DirectoryPicked(val path: String) : AppMsg

    /** Driver publishes this after persisting the new profile and resolving the name. */
    data class ProfileCurrentResolved(val data: ProfileData) : AppMsg

    /** View publishes this when user changes theme via the button. */
    data class ThemeChangeRequested(val theme: IntUiThemes) : AppMsg

    /** Driver publishes this after theme is persisted. */
    data class ThemeChanged(val data: ProfileData) : AppMsg

    // -----------------------------------------------------------------------
    // Window
    // -----------------------------------------------------------------------

    data class WindowLoaded(val data: WindowData) : AppMsg

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    data object NavigateToWelcome : AppMsg
    data object ShowSettings : AppMsg
    data object HideSettings : AppMsg
    data object ShowNewModpack : AppMsg
    data object HideNewModpack : AppMsg

    // -----------------------------------------------------------------------
    // Close / Quit dialog
    // -----------------------------------------------------------------------

    data class RequestCloseDialog(val request: CloseDialogRequest) : AppMsg
    data object DismissCloseDialog : AppMsg
    data object ConfirmCloseDialog : AppMsg

    /** The driver publishes this after saving window data — signals actual quit. */
    data object QuitReady : AppMsg

    // -----------------------------------------------------------------------
    // Pro
    // -----------------------------------------------------------------------

    data class ProActivationChecked(val activated: Boolean?) : AppMsg

    // -----------------------------------------------------------------------
    // Modpack messages
    // -----------------------------------------------------------------------

    sealed interface Modpack : AppMsg {
        /** Driver loaded fresh modpack data from disk. */
        data class Loaded(
            val lockFile: Result<LockFile, ActionError>,
            val configFile: Result<ConfigFile, ActionError>,
        ) : Modpack

        data object Reset : Modpack

        data class TabSelected(val tab: SelectedTab) : Modpack
        data class ProjectSelected(val project: Project?) : Modpack
        data class ProjectEditing(val editing: Boolean) : Modpack

        data class ProjectsSelected(val pakkuIds: Set<String>) : Modpack
        data class ProjectsDeselected(val pakkuIds: Set<String>) : Modpack
        data class ProjectsCleared(val dummy: Unit = Unit) : Modpack

        data class SortOrderChanged(val order: SortOrder) : Modpack
        data class FilterTextChanged(val text: String) : Modpack

        /** View requests export — driver will pick up on model.modpack.wantsExport. */
        data object ExportRequested : Modpack

        /** Driver publishes this as soon as the action coroutine has started. */
        data class ActionStarted(val name: String) : Modpack

        /** Driver publishes this when action coroutine completes or is cancelled. */
        data object ActionFinished : Modpack

        /** View publishes this to request termination of the running action. */
        data object TerminateAction : Modpack

        data class ToastAdded(val toast: ToastData) : Modpack
        data class ToastDismissed(val id: String) : Modpack
    }
}

