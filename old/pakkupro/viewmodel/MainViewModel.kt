package pakkupro.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import teksturepako.pakkupro.data.ProfileData
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.pathString
import com.github.michaelbull.result.fold as resultFold

object MainViewModel {
    val profileData = ProfileData.readOrNew()

    var theme: IntUiThemes by mutableStateOf(profileData.intUiTheme)

    lateinit var lockFile: LockFile
    var configFile: ConfigFile? = null

    fun updatePakkuApi(): String? = runBlocking {

        val errors = StringBuilder()

        workingPath = profileData.currentProfile?.pathString.toString()

        LockFile.readToResult().fold(
            onSuccess = {
                lockFile = it
            },
            onFailure = {
                errors.append(it.message + "\n")
            }
        )
        ConfigFile.readToResult().resultFold(
            success = {
                configFile = it
            },
            failure = {
                configFile = null
                errors.append(it.rawMessage + "\n")
            }
        )

        return@runBlocking if (errors.isNotBlank()) errors.toString() else null
    }

    init
    {
        profileData.write()
    }
}