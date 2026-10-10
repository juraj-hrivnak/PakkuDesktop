/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.getOrElse
import io.klogging.logger
import kotlinx.coroutines.joinAll
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyExists
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
 * Pure suspend export — [onToast] from actionDriver.
 */
suspend fun exportSuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    onToast: suspend (ToastData) -> Unit,
) {
    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return
    }

    exportDefaultProfiles(
        onError = { profile: ExportProfile, error: ActionError ->
            // Same as CLI: skip AlreadyExists (export file already present).
            if (error is AlreadyExists) return@exportDefaultProfiles
            logger.error { "[${profile.name} profile] ${error.toUiMessage()}" }
            onToast(actionErrorToast("[${profile.name} profile]", error))
        },
        onSuccess = { profile: ExportProfile, path: Path, duration: Duration ->
            val fileSize = path.fileSize().toHumanReadableSize()
            val filePath = Path(workingPath).relativize(path).pathString
            logger.info { "[${profile.name} profile] exported to '$filePath' ($fileSize) in ${duration.shortForm()}" }

            onToast(
                actionExportedToast(
                    profileName = profile.name,
                    filePath = filePath,
                    fileSize = fileSize,
                    duration = duration.shortForm(),
                    directory = path.parent.absolutePathString(),
                )
            )
        },
        lockFile, configFile, platforms,
    ).joinAll()
}
