/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.IOExportingError
import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.exportDefaultProfiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import teksturepako.pakkuDesktop.app.io.RevealFileAction
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.pathString
import kotlin.time.Duration

/**
 * Pure suspend export implementation — no ViewModels, no global state.
 * The [onToast] callback is provided by the actionDriver to surface results.
 */
suspend fun exportSuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    onToast: suspend (ToastData) -> Unit,
) {
    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(errorToast(error.rawMessage))
        return
    }

    exportDefaultProfiles(
        onError = { profile: ExportProfile, error: ActionError ->
            if (error !is IOExportingError) {
                val message = "[${profile.name} profile] ${error.rawMessage}"
                println(message)
                onToast(errorToast("[${profile.name} profile]\n${error.rawMessage}"))
            }
        },
        onSuccess = { profile: ExportProfile, path: Path, duration: Duration ->
            val fileSize = path.fileSize().toHumanReadableSize()
            val filePath = Path(workingPath).relativize(path).pathString
            println("[${profile.name} profile] exported to '$filePath' ($fileSize) in ${duration.shortForm()}")

            onToast(ToastData(content = {
                Box(Modifier.padding(16.dp).width(300.dp)) {
                    Column {
                        Text("[${profile.name} profile]", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("exported to '$filePath'", style = JewelTheme.consoleTextStyle)
                        Text(" ($fileSize) in ${duration.shortForm()}")
                        Spacer(Modifier.height(8.dp))
                        DefaultButton(onClick = { RevealFileAction.openFile(path) }) {
                            Text("Open")
                        }
                    }
                }
            }))
        },
        lockFile, configFile, platforms,
    ).joinAll()
}

private fun errorToast(message: String) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Text(message)
    }
})
