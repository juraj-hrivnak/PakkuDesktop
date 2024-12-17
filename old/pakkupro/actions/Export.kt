package pakkupro.actions

import androidx.compose.runtime.MutableState
import com.dokar.sonner.TextToastAction
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import com.github.michaelbull.result.get
import kotlinx.coroutines.*
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.export.export
import teksturepako.pakku.api.actions.export.profiles.CurseForgeProfile
import teksturepako.pakku.api.actions.export.profiles.ModrinthProfile
import teksturepako.pakku.api.actions.export.profiles.ServerPackProfile
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import java.awt.Desktop
import java.util.concurrent.Executors
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize
import kotlin.time.Duration.Companion.seconds

/** Safeguard for not enabling exporting on re-composition. */
private var exporting = false

fun export(
    enabled: MutableState<Boolean>,
    toasterState: ToasterState,
)
{
    if (!enabled.value || exporting) return

    exporting = true

    val threadPool = Executors.newSingleThreadExecutor {
        task -> Thread(task, "export-background-thread")
    }.asCoroutineDispatcher() + SupervisorJob()

    fun message(message: String, type: ToastType, action: TextToastAction? = null)
    {
        println(message)
        toasterState.show(Toast(message = message, type = type, duration = 30.seconds, action = action))
    }

    CoroutineScope(threadPool).launch {

        val lockFile = LockFile.readToResult().getOrNull() ?: return@launch
        val configFile = ConfigFile.readToResult().get() ?: return@launch
        val platforms: List<Platform> = lockFile.getPlatforms().getOrNull() ?: return@launch

        export(
            profiles = listOf(
                CurseForgeProfile(lockFile, configFile),
                ModrinthProfile(lockFile, configFile),
                ServerPackProfile()
            ),
            onError = { profile, error ->
                if (error !is ActionError.AlreadyExists)
                {
                    message("[${profile.name} profile] ${error.rawMessage}", ToastType.Error)
                }
            },
            onSuccess = { profile, file, duration ->
                val fileSize = file.fileSize().toHumanReadableSize()
                val filePath = '.' + file.absolutePathString()
                    .substringAfter(MainViewModel.profileData.currentProfile.toString())

                val message = "[${profile.name} profile] exported to '$filePath' ($fileSize) in ${duration.shortForm()}"

                message(message, ToastType.Success, action = TextToastAction("Open", onClick = {
                    runCatching {
                        Desktop.getDesktop().open(file.parent.toFile())
                    }
                }))
            },
            lockFile, configFile, platforms
        ).joinAll()

        enabled.value = false
        exporting = false
    }
}
