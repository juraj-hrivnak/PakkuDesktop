/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pro.git.wrapper.NativeGit
import java.io.File

/**
 * Clones a repository when [AppModel.pendingClone] is set, then opens the folder
 * via [AppMsg.DirectoryPicked] on success.
 */
val cloneDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.pendingClone) {
        val request = model.pendingClone ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            NativeGit.clone(request.url.trim(), File(request.destPath))
        }
        result.fold(
            onSuccess = { dest ->
                publish(AppMsg.CloneFinished(errorMessage = null))
                publish(AppMsg.DirectoryPicked(dest.absolutePath))
            },
            onFailure = { error ->
                publish(AppMsg.CloneFinished(errorMessage = error.message ?: "Clone failed."))
            },
        )
    }

    content()
}
