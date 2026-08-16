/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

// -- projectEditDriver --

/**
 * Watches [AppModel.modpack]'s pending property write. When non-null:
 * 1. Writes the project config change to disk.
 * 2. Re-reads LockFile + ConfigFile from disk.
 * 3. Publishes [ModpackMsg.Loaded] to update the model.
 * 4. Publishes [ModpackMsg.PropertyWriteCompleted] to clear the pending write.
 */
val projectEditDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.modpack.pendingPropertyWrite) {
        val request    = model.modpack.pendingPropertyWrite ?: return@LaunchedEffect
        val lockFile   = model.modpack.lockFile?.get()      ?: return@LaunchedEffect
        val configFile = model.modpack.configFile?.get()    ?: return@LaunchedEffect
        val project    = model.modpack.selectedProject      ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            configFile.setProjectConfig(project, lockFile, request.write)
            configFile.write()
        }

        val newLockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val newConfigFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile)))
        publish(AppMsg.Modpack(ModpackMsg.PropertyWriteCompleted))
    }

    LaunchedEffect(model.modpack.pendingMetaWrite) {
        val request    = model.modpack.pendingMetaWrite ?: return@LaunchedEffect
        val lockFile   = model.modpack.lockFile?.get()   ?: return@LaunchedEffect
        val configFile = model.modpack.configFile?.get() ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            request.mutate(configFile, lockFile)
            configFile.write()
            lockFile.write()
        }

        val newLockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val newConfigFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile)))
        publish(AppMsg.Modpack(ModpackMsg.MetaWriteCompleted))
    }

    content()
}



