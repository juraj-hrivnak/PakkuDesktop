package teksturepako.pakkupro.ui.viewmodel

import com.github.michaelbull.result.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkupro.data.ProfileData
import teksturepako.pakkupro.data.ProfileData.CloseDialogData
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import java.nio.file.Path
import kotlin.io.path.Path

object ProfileViewModel
{
    private var _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()

    init
    {
        loadFromDisk()
    }

    fun loadFromDisk()
    {
        val profileDataState = ProfileData.readOrNew()

        println("ProfileViewModel loaded from disk")

        _profileData.update {
            profileDataState
        }
    }

    suspend fun writeToDisk()
    {
        println("ProfileViewModel written to disk")

        // Write to disk
        _profileData.value.write()
    }

    private fun addRecentProfile(path: Path) = runBlocking {
        val modpackName = ConfigFile.readToResultFrom(Path("$path/${ConfigFile.FILE_NAME}"))
            .get()?.getName() ?: return@runBlocking

        if (path.toString() !in _profileData.value.recentProfiles)
        {
            _profileData.update { currentState ->
                currentState.copy(
                    recentProfiles = currentState.recentProfiles.plus(
                        modpackName to path.toString()
                    )
                )
            }
        }
    }

    suspend fun updateCurrentProfile(updatedCurrentProfile: Path?)
    {
        loadFromDisk()

        _profileData.value.currentProfilePath?.let { addRecentProfile(it) }

        if (updatedCurrentProfile == null)
        {
            _profileData.update { currentState ->
                currentState.copy(
                    currentProfile = null
                )
            }

            // Update Pakku's working path
            workingPath = "."
        }
        else
        {
            _profileData.update { currentState ->
                currentState.copy(
                    currentProfile = updatedCurrentProfile.toString()
                )
            }

            // Update Pakku's working path
            workingPath = updatedCurrentProfile.toString()
        }

        writeToDisk()

        ModpackViewModel.reset()
    }

    suspend fun updateTheme(updatedTheme: IntUiThemes)
    {
        loadFromDisk()

        _profileData.update { currentState ->
            currentState.copy(
                theme = updatedTheme.toString()
            )
        }

        writeToDisk()
    }

    // -- CLOSE DIALOG --

    fun updateCloseDialog(forceClose: Boolean = false, onClose: suspend () -> Unit)
    {
        _profileData.update { currentState ->
            currentState.copy(
                closeDialog = CloseDialogData(onClose, forceClose)
            )
        }
    }

    fun dismissCloseDialog()
    {
        _profileData.update { currentState ->
            currentState.copy(
                closeDialog = null
            )
        }
    }
}