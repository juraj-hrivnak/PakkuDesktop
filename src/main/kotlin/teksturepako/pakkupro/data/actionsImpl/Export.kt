package teksturepako.pakkupro.data.actionsImpl

import com.dokar.sonner.ToastType
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.exportDefaultProfiles
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import teksturepako.pakkupro.data.ProfileData
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.state.ModpackUiState
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.fileSize
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ExportData(
    val profile: ExportProfile, val path: Path, val duration: Duration
)

fun exportImpl(profileData: ProfileData, modpackUiState: ModpackUiState)
{
    if (modpackUiState.action.first != null) return

    try
    {
        val threadPool = Executors.newSingleThreadExecutor { thread ->
            Thread(thread, "export-background-thread").apply {
                // Set to daemon to prevent hanging
                isDaemon = true
            }
        }.asCoroutineDispatcher() + SupervisorJob()

        val job = CoroutineScope(threadPool + Dispatchers.IO).launch {
            val lockFile = modpackUiState.lockFile?.copy() ?: return@launch
            val configFile = modpackUiState.configFile?.copy() ?: return@launch
            val platforms = lockFile.getPlatforms().getOrNull() ?: return@launch

            exportDefaultProfiles(
                onError = { profile: ExportProfile, error: ActionError ->
                    if (error !is AlreadyExists) {
                        val message = "[${profile.name} profile] ${error.rawMessage}"
                        withContext(Dispatchers.Main) {
                            ModpackViewModel.toasterState?.show(
                                message,
                                type = ToastType.Error,
                                duration = 30.seconds
                            )
                        }
                        println(message)
                    }
                },
                onSuccess = { profile: ExportProfile, path: Path, duration: Duration ->
                    val fileSize = path.fileSize().toHumanReadableSize()
                    val filePath = path.pathString

                    val message = "[${profile.name} profile] exported to '$filePath' " +
                            "($fileSize) in ${duration.shortForm()}"

                    withContext(Dispatchers.Main) {
                        ModpackViewModel.toasterState?.show(
                            ExportData(profile, path, duration),
                            type = ToastType.Success,
                            duration = 30.seconds
                        )
                    }
                    println(message)
                },
                lockFile = lockFile,
                configFile = configFile,
                platforms = platforms,
            ).joinAll()
        }

        ModpackViewModel.updateAction("Exporting", job)

        job.invokeOnCompletion { throwable ->
            ModpackViewModel.updateAction(null, null)

            throwable?.let { error ->
                ModpackViewModel.toasterState?.show(
                    "Export failed: ${error.message}",
                    type = ToastType.Error,
                    duration = 30.seconds
                )
            }
        }
    }
    catch (e: Exception) {
        // Reset state if something goes wrong during setup
        ModpackViewModel.updateAction(null, null)
    }
}
