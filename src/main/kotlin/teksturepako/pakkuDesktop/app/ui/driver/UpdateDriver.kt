/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.provider.GitHubProvider
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.data.AppInfo
import teksturepako.pakkuDesktop.app.data.UpdateChannel
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

private val logger = logger("UpdateDriver")

val updateDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val scope = rememberCoroutineScope()
    var installerFile by remember { mutableStateOf<java.io.File?>(null) }
    val channel = model.profile.data.updateChannelEnum
    val updater by remember(channel) {
        mutableStateOf(runCatching { createUpdater(channel) }.getOrNull())
    }

    // -- Check for updates --
    LaunchedEffect(model.wantsCheckForUpdates) {
        if (!model.wantsCheckForUpdates) return@LaunchedEffect
        val updater = updater ?: run {
            publish(AppMsg.UpdateCheckFinished(errorMessage = "Auto-update is not available in this build."))
            return@LaunchedEffect
        }

        if (!updater.isUpdateSupported()) {
            publish(AppMsg.UpdateCheckFinished(errorMessage = "Auto-update is not supported for this build type."))
            return@LaunchedEffect
        }

        publish(AppMsg.UpdateCheckFinished(availableVersion = null, errorMessage = null))

        val result = withContext(Dispatchers.IO) {
            updater.checkForUpdates()
        }

        when (result) {
            is UpdateResult.Available -> {
                logger.info { "Update available: ${result.info.version}" }
                publish(AppMsg.UpdateCheckFinished(availableVersion = result.info.version))

                // Start download automatically
                scope.launch {
                    try {
                        updater.downloadUpdate(result.info).collect { progress ->
                            val pct = if (progress.totalBytes > 0) {
                                (progress.bytesDownloaded.toDouble() / progress.totalBytes).toFloat()
                            } else 0f
                            publish(AppMsg.UpdateDownloadProgress(pct.coerceIn(0f, 1f)))
                            if (progress.file != null) {
                                installerFile = progress.file
                            }
                        }
                        publish(AppMsg.UpdateDownloadFinished)
                    } catch (e: Exception) {
                        logger.warn(e) { "Download failed: ${e.message}" }
                        publish(AppMsg.UpdateCheckFinished(errorMessage = "Download failed: ${e.message}"))
                    }
                }
            }
            is UpdateResult.NotAvailable -> {
                logger.info { "No update available" }
                publish(AppMsg.UpdateCheckFinished(availableVersion = null, errorMessage = null))
            }
            is UpdateResult.Error -> {
                logger.warn(result.exception) { "Update check failed" }
                publish(AppMsg.UpdateCheckFinished(
                    errorMessage = result.exception.message ?: "Failed to check for updates."
                ))
            }
        }
    }

    // -- Install and restart --
    LaunchedEffect(model.updateUserConfirmedInstall && installerFile != null) {
        val file = installerFile ?: return@LaunchedEffect
        if (!model.updateUserConfirmedInstall) return@LaunchedEffect
        val updater = updater ?: return@LaunchedEffect

        logger.info { "Installing update from ${file.path}" }
        publish(AppMsg.UpdateInstalling)
        try {
            updater.installAndRestart(file)
        } catch (e: Exception) {
            logger.warn(e) { "Install failed: ${e.message}" }
            publish(AppMsg.UpdateCheckFinished(errorMessage = "Install failed: ${e.message}"))
        }
    }

    content()
}

private fun createUpdater(channel: UpdateChannel): NucleusUpdater {
    return NucleusUpdater {
        provider = GitHubProvider(
            owner = "juraj-hrivnak",
            repo = "PakkuDesktop",
        )
        currentVersion = AppInfo.VERSION
        differentialDownload = true
        this.channel = channel.id
    }
}
