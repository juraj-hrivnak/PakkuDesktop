/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.getOrElse
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitOpenFileSettings
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.klogging.logger
import kotlinx.coroutines.joinAll
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
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize
import kotlin.io.path.pathString
import kotlin.time.Duration

val logger = logger("ExportImpl")

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
                logger.error(message)
                onToast(errorToast("[${profile.name} profile]\n${error.rawMessage}"))
            }
        },
        onSuccess = { profile: ExportProfile, path: Path, duration: Duration ->
            val fileSize = path.fileSize().toHumanReadableSize()
            val filePath = Path(workingPath).relativize(path).pathString
            logger.info { "[${profile.name} profile] exported to '$filePath' ($fileSize) in ${duration.shortForm()}" }

            onToast(ToastData(content = {
                Box(Modifier.padding(16.dp).width(300.dp)) {
                    Column {
                        Text("[${profile.name} profile]", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("exported to '$filePath'", style = JewelTheme.consoleTextStyle)
                        Text(" ($fileSize) in ${duration.shortForm()}")
                        Spacer(Modifier.height(8.dp))
                        DefaultButton(onClick = {
                            val file = PlatformFile(path.parent.absolutePathString())
                            FileKit.openFileWithDefaultApplication(file)
                        }) {
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
