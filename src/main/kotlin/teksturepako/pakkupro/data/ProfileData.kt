package teksturepako.pakkupro.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.io.writeToFile
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class ProfileData(
    val currentProfile: String? = null,
    val recentProfiles: Map<String, String> = mutableMapOf(),
    val theme: String = "Dark",

    @Transient val closeDialog: CloseDialogData? = null
)
{
    val currentProfilePath: Path?
        get() = currentProfile?.let { Path(it) }

    val recentProfilesFiltered: Map<String, String>
        get() = recentProfiles.filterNot {
            it.value == currentProfile.toString()
        }

    val intUiTheme
        get() = IntUiThemes.valueOf(this.theme)

    data class CloseDialogData(
        val onClose: suspend () -> Unit,
        val forceClose: Boolean = false
    )

    companion object
    {
        const val FILE_NAME = "profile-data.json"

        fun readOrNew(): ProfileData
        {
            return runCatching {
                jsonEncodeDefaults.decodeFromString<ProfileData>(File(FILE_NAME).readText())
            }.getOrNull() ?: ProfileData()
        }
    }

    suspend fun write(): ActionError? = writeToFile<ProfileData>(
        this, FILE_NAME, format = jsonEncodeDefaults
    )
}
