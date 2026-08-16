/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes

sealed interface AppMsg {

    // -- Profile --

    data class ProfileLoaded(val data: ProfileData) : AppMsg
    data class ProfileCurrentResolved(val data: ProfileData) : AppMsg
    data class ThemeChangeRequested(val theme: IntUiThemes) : AppMsg
    data class ThemeChanged(val data: ProfileData) : AppMsg

    /** directoryPickerDriver */
    data class DirectoryPicked(val path: String) : AppMsg

    /** open OS directory picker */
    data object OpenDirectoryPickerRequested : AppMsg

    /** directoryPickerDriver consumed the request */
    data object DirectoryPickerLaunched : AppMsg

    // -- Window --

    data class WindowLoaded(val data: WindowData) : AppMsg

    // -- Dialog dismissals --

    data object HideSettings : AppMsg
    data object HideNewModpack : AppMsg

    /** credentialsDriver finished loading settings form values */
    data class CredentialsLoaded(
        val curseForgeApiKey: String,
        val gitHubAccessToken: String,
    ) : AppMsg

    /** credentialsDriver */
    data class CredentialsUpdateRequested(
        val curseForgeApiKey: String,
        val gitHubAccessToken: String,
    ) : AppMsg

    /** credentialsDriver finished writing credentials */
    data class CredentialsUpdateHandled(val statusMessage: String) : AppMsg

    data object ShowActivation : AppMsg
    data object HideActivation : AppMsg

    /** cloneDriver: clone [url] into [destPath], then open */
    data class CloneRequested(val url: String, val destPath: String) : AppMsg
    data class CloneFinished(val errorMessage: String?) : AppMsg
    data object ShowCloneDialog : AppMsg
    data object HideCloneDialog : AppMsg
    /** clone dialog Browse… */
    data object CloneParentPickerRequested : AppMsg
    data class CloneParentPicked(val path: String) : AppMsg
    data object CloneParentPickerLaunched : AppMsg

    // -- Close / Quit dialog --

    data class RequestCloseDialog(val request: CloseDialogRequest) : AppMsg
    data object DismissCloseDialog : AppMsg
    data object ConfirmCloseDialog : AppMsg

    /** after windowDiskDriver save — actually quit */
    data object QuitReady : AppMsg

    // -- Pro --

    data class ProActivationChecked(val activated: Boolean?) : AppMsg

    /** licenseDriver */
    data class LicenseKeySubmit(val key: String) : AppMsg

    data class LicenseKeyHandled(
        val activated: Boolean?,
        val error: ActionError?,
    ) : AppMsg

    // -- Child components --

    data class Welcome(val msg: WelcomeMsg) : AppMsg
    data class Modpack(val msg: ModpackMsg) : AppMsg
}
