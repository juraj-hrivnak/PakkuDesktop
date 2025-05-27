/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.viewmodel

import com.github.michaelbull.result.get
import io.klogging.Klogger
import io.klogging.logger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.data.Profile
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.ProfileData.CloseDialogData
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

object ProfileViewModel
{
    private val logger: Klogger = logger(this::class)

    private var _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()

    suspend fun loadFromDisk(updateWorkingPath: Boolean = true) = coroutineScope {

        val updatedProfileData = ProfileData.readOrNew()

        logger.info { "loaded from disk" }

        _profileData.update { currentState ->
            currentState.copy(
                currentProfile = updatedProfileData.currentProfile,
                recentProfiles = updatedProfileData.recentProfiles,
                theme = updatedProfileData.theme,
                closeDialog = currentState.closeDialog
            )
        }

        if (updateWorkingPath)
        {
            launch {
                _profileData.value.currentProfile?.path.let {
                    workingPath = it ?: "."
                    logger.info { "workingPath set to [$workingPath]" }
                }
            }.join()
        }
    }

    suspend fun writeToDisk()
    {
        logger.info { "written to disk" }

        // Write to disk
        _profileData.value.write()
    }

    private fun addRecentProfile(path: Path) = runBlocking {
        val modpackName = ConfigFile.readToResultFrom(Path("$path/${ConfigFile.FILE_NAME}"))
            .get()?.getName() ?: path.fileName.pathString

        if (path.absolutePathString() !in _profileData.value.recentProfiles.map { it.path })
        {
            // Don't teksturepako.pakkuDesktop.actions.add the profile to recent profiles if it doesn't have a lock file.
            if (LockFile.readToResultFrom(Path(path.pathString, LockFile.FILE_NAME)).isErr) return@runBlocking

            _profileData.update { currentState ->
                currentState.copy(
                    recentProfiles = currentState.recentProfiles.plus(
                        Profile(
                            name = modpackName,
                            path = path.absolutePathString(),
                            lastOpened = Clock.System.now()
                        )
                    )
                )
            }
        }
        else
        {
            _profileData.update { currentState ->
                val updatedState = currentState.recentProfiles.map { profile ->
                    if (profile.path != path.absolutePathString()) return@map profile // Return the same profile

                    Profile(
                        name = modpackName,
                        path = path.absolutePathString(),
                        lastOpened = Clock.System.now()
                    )
                }

                currentState.copy(
                    recentProfiles = updatedState
                )
            }
        }
    }

    suspend fun updateCurrentProfile(updatedCurrentProfile: Path?) = coroutineScope {

        loadFromDisk(updateWorkingPath = false)

        _profileData.value.currentProfilePath?.let { addRecentProfile(it) }

        if (updatedCurrentProfile == null)
        {
            _profileData.update { currentState ->
                currentState.copy(
                    currentProfile = null
                )
            }
        }
        else
        {
            val modpackName = ConfigFile.readToResultFrom(Path("$updatedCurrentProfile/${ConfigFile.FILE_NAME}"))
                .get()?.getName() ?: updatedCurrentProfile.fileName.pathString

            _profileData.update { currentState ->
                currentState.copy(
                    currentProfile = Profile(
                        name = modpackName,
                        path = updatedCurrentProfile.absolutePathString(),
                        lastOpened = Clock.System.now()
                    )
                )
            }
        }

        launch {
            _profileData.value.currentProfile?.path.let {
                workingPath = it ?: "."
                logger.info { "workingPath set to [$workingPath]" }
            }
        }.join()

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