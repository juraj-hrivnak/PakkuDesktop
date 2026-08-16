/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import io.klogging.logger
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val fetchLogger = logger("FetchImpl")

/**
 * Pure suspend fetch — mirrors CLI `pakku fetch`.
 * [onToast] from actionDriver.
 */
suspend fun fetchSuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    onToast: suspend (ToastData) -> Unit,
) {
    val results = retrieveProjectFiles(lockFile, Provider.providers)
    val projectFiles = results.mapNotNull { result ->
        result.onFailure { error ->
            fetchLogger.error(error.toUiMessage())
            onToast(actionErrorToast(error))
        }.get()
    }

    if (projectFiles.isEmpty()) {
        onToast(actionInfoToast("Nothing to fetch."))
        return
    }

    var successCount = 0
    projectFiles.fetch(
        onError = { error ->
            // CLI skips AlreadyExists (file already on disk) — don't spam toasts.
            if (error is AlreadyExists) {
                fetchLogger.info { error.toUiMessage() }
                return@fetch
            }
            fetchLogger.error(error.toUiMessage())
            onToast(actionErrorToast(error))
        },
        onProgress = { _, _ -> },
        onSuccess = { _, projectFile ->
            successCount++
            fetchLogger.info { "Fetched ${projectFile.fileName}" }
        },
        lockFile = lockFile,
        configFile = configFile,
    ).join()

    if (successCount > 0) {
        onToast(actionInfoToast("Fetched $successCount file${if (successCount == 1) "" else "s"}."))
    }
}
