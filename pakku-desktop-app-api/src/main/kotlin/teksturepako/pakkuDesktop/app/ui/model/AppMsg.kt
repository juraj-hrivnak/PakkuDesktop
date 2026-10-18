/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.app.data.HeapFootprint
import teksturepako.pakkuDesktop.app.data.HeapHistogram
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes

sealed interface AppMsg {

    // -- Profile --

    data class ProfileLoaded(val data: ProfileData) : AppMsg
    data class ProfileCurrentResolved(val data: ProfileData) : AppMsg
    data class ThemeChangeRequested(val theme: IntUiThemes) : AppMsg
    data class ThemeChanged(val data: ProfileData) : AppMsg
    data class UiScaleChangeRequested(val uiScale: Float) : AppMsg

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
    data class SettingsTabSelected(val tab: SettingsTab) : AppMsg
    data object HeapHistogramRequested : AppMsg
    data class HeapHistogramLoaded(val histogram: HeapHistogram) : AppMsg
    data class HeapFootprintSampled(val footprint: HeapFootprint, val action: String? = null) : AppMsg
    data object HideNewModpack : AppMsg

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

    // -- Auto-update --

    /** User clicked "Check for updates" in Settings */
    data object CheckForUpdatesRequested : AppMsg

    /** updateDriver: check completed */
    data class UpdateCheckFinished(val availableVersion: String? = null, val errorMessage: String? = null) : AppMsg

    /** updateDriver: download progress */
    data class UpdateDownloadProgress(val progress: Float) : AppMsg

    /** updateDriver: download finished, installer ready */
    data object UpdateDownloadFinished : AppMsg

    /** User confirmed install and restart */
    data object UpdateInstallRequested : AppMsg

    /** updateDriver: install started (app will exit) */
    data object UpdateInstalling : AppMsg

    /** Dismiss update status */
    data object DismissUpdateStatus : AppMsg

    // -- Internal --

    /**
     * Credentials and license. Only the host and [teksturepako.pakkuDesktop.plugin.PluginTrust.Official]
     * plugins may publish or observe these.
     */
    data class Internal(val msg: InternalMsg) : AppMsg

    // -- Child components --

    data class Welcome(val msg: WelcomeMsg) : AppMsg
    data class Modpack(val msg: ModpackMsg) : AppMsg
    data class Plugin(val id: PluginId, val msg: Any?) : AppMsg
}

/** Messages that carry secrets or license state; always wrapped in [AppMsg.Internal]. */
sealed interface InternalMsg {
    /** credentialsDriver finished loading settings form values */
    data class CredentialsLoaded(
        val curseForgeApiKey: String,
        val gitHubAccessToken: String,
    ) : InternalMsg

    /** credentialsDriver */
    data class CredentialsUpdateRequested(
        val curseForgeApiKey: String,
        val gitHubAccessToken: String,
    ) : InternalMsg

    /** pakku-pro finished a local activation check or a key submit. */
    data class ProActivationReported(val activated: Boolean) : InternalMsg
}
