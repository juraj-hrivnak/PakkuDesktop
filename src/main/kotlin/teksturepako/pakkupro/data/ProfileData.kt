package teksturepako.pakkupro.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class ProfileData(
    val currentProfile: String? = null,
    val recentProfiles: Map<String, String> = mutableMapOf(),
    val theme: String = "Dark",
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

    fun write()
    {
        val text = jsonEncodeDefaults.encodeToString(json.serializersModule.serializer(), this)
        File(FILE_NAME).writeText(text)
    }
}
