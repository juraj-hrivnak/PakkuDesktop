/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.io.readPathTextOrNull
import teksturepako.pakku.io.writeToFile
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class ProfileData(
    @SerialName("current_profile") val currentProfile: Profile? = null,
    @SerialName("recent_profiles") val recentProfiles: List<Profile> = listOf(),
    val theme: String = "Dark",
    @SerialName("ui_scale") val uiScale: Float = UI_SCALE_DEFAULT,

    @Transient val closeDialog: CloseDialogData? = null
)
{
    val currentProfilePath: Path?
        get() = currentProfile?.let { Path(it.path) }

    val recentProfilesFiltered: List<Profile>
        get() = recentProfiles
            .filterNot { recentProfiles ->
                recentProfiles.path == currentProfile?.path
            }
            .sortedByDescending { it.lastOpened }

    val intUiTheme
        get() = IntUiThemes.valueOf(this.theme)

    data class CloseDialogData(
        val onClose: suspend () -> Unit,
        val forceClose: Boolean = false
    )

    companion object
    {
        const val FILE_NAME = "profile-data.json"
        const val UI_SCALE_MIN = 0.75f
        const val UI_SCALE_MAX = 2f
        const val UI_SCALE_DEFAULT = 1f
        val UI_SCALE_PRESETS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

        fun coerceUiScale(value: Float): Float = value.coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)

        suspend fun readOrNew(): ProfileData
        {
            val text = readPathTextOrNull(Path(FILE_NAME)) ?: return ProfileData()

            return runCatching { jsonEncodeDefaults.decodeFromString<ProfileData>(text) }.getOrNull() ?: ProfileData()
        }
    }

    suspend fun write(): ActionError? = writeToFile<ProfileData>(
        this, FILE_NAME, format = jsonEncodeDefaults
    )
}
