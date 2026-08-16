/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

/**
 * Credentials IO for the settings dialog.
 *
 * - [AppModel.wantsLoadCredentials] → read ~/.pakku/credentials → [AppMsg.CredentialsLoaded]
 * - [AppModel.pendingCredentialsUpdate] → write → [AppMsg.CredentialsUpdateHandled]
 */
val credentialsDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.wantsLoadCredentials) {
        if (!model.wantsLoadCredentials) return@LaunchedEffect
        val creds = withContext(Dispatchers.IO) { CredentialsFile.readToResult().get() }
        publish(
            AppMsg.CredentialsLoaded(
                curseForgeApiKey = creds?.curseForgeApiKey.orEmpty(),
                gitHubAccessToken = creds?.gitHubAccessToken.orEmpty(),
            ),
        )
    }

    LaunchedEffect(model.pendingCredentialsUpdate) {
        val request = model.pendingCredentialsUpdate ?: return@LaunchedEffect
        val error = withContext(Dispatchers.IO) {
            CredentialsFile.update(
                updatedCurseForgeApiKey = request.curseForgeApiKey.ifBlank { null },
                updatedGitHubAccessToken = request.gitHubAccessToken.ifBlank { null },
            )
        }
        publish(
            AppMsg.CredentialsUpdateHandled(
                statusMessage = error?.rawMessage ?: "Saved credentials.",
            ),
        )
    }

    content()
}
