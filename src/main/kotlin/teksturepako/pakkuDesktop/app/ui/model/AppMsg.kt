/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes

sealed interface AppMsg {

    // -----------------------------------------------------------------------
    // Profile (dispatched by profileDiskDriver)
    // -----------------------------------------------------------------------

    data class ProfileLoaded(val data: ProfileData) : AppMsg
    data class ProfileCurrentResolved(val data: ProfileData) : AppMsg
    data class ThemeChangeRequested(val theme: IntUiThemes) : AppMsg
    data class ThemeChanged(val data: ProfileData) : AppMsg

    /** Dispatched by directoryPickerDriver when the OS file picker returns. */
    data class DirectoryPicked(val path: String) : AppMsg

    // -----------------------------------------------------------------------
    // Window (dispatched by windowDiskDriver)
    // -----------------------------------------------------------------------

    data class WindowLoaded(val data: WindowData) : AppMsg

    // -----------------------------------------------------------------------
    // Dialog dismissals — dispatched from AppView lambdas
    // -----------------------------------------------------------------------

    data object HideSettings : AppMsg
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
    // Pro (dispatched by licenseDriver)
    // -----------------------------------------------------------------------

    data class ProActivationChecked(val activated: Boolean?) : AppMsg

    /** Dispatched from the activation UI; fulfilled by licenseDriver. */
    data class LicenseKeySubmit(val key: String) : AppMsg

    data class LicenseKeyHandled(
        val activated: Boolean?,
        val error: ActionError?,
    ) : AppMsg

    // -----------------------------------------------------------------------
    // Child component message wrappers (fractal delegation)
    // -----------------------------------------------------------------------

    /** All messages originating from the Welcome screen. */
    data class Welcome(val msg: WelcomeMsg) : AppMsg

    /** All messages originating from the Modpack screen. */
    data class Modpack(val msg: ModpackMsg) : AppMsg
}
