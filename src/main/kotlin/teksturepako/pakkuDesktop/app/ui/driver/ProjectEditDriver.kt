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
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.actions.writeKeepingUnknownKeys
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.PackMetaSettingError
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectConfigMapKey
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import kotlin.io.path.Path
import kotlin.io.path.readText

// -- projectEditDriver --

/**
 * Metadata writes ([ModpackModel.pendingMetaWrites]) run one at a time, oldest first.
 * Each write updates `pakku.json` / `pakku-lock.json`, then re-reads both files.
 */
val projectEditDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.modpack.pendingPropertyWrite, model.modpack.pendingPropertyWriteProjectKey) {
        val request = model.modpack.pendingPropertyWrite ?: return@LaunchedEffect
        val projectKey = model.modpack.pendingPropertyWriteProjectKey ?: return@LaunchedEffect
        val errorFieldKey = model.modpack.pendingPropertyWriteErrorKey

        fun finish(errors: List<teksturepako.pakku.api.actions.errors.ActionError>) {
            publish(AppMsg.Modpack(ModpackMsg.PropertyWriteCompleted))
            if (errors.isNotEmpty()) {
                publish(AppMsg.Modpack(ModpackMsg.MetaWriteFailed(fieldKey = errorFieldKey, errors = errors)))
            }
        }

        val lockFile = model.modpack.lockFile?.get()
        val configFile = model.modpack.configFile?.get()
        if (lockFile == null || configFile == null) {
            finish(emptyList())
            return@LaunchedEffect
        }

        val project = lockFile.getAllProjects().find { it.uiKey() == projectKey }
        if (project == null) {
            finish(listOf(PackMetaSettingError("Could not find that project in the lock file.")))
            return@LaunchedEffect
        }

        val mapKey = projectConfigMapKey(project, configFile)
        if (mapKey == null) {
            finish(listOf(PackMetaSettingError("This project has no slug or name to store overrides under.")))
            return@LaunchedEffect
        }

        val writeErrors = withContext(Dispatchers.IO) {
            try {
                listOfNotNull(
                    configFile.setProjectConfig(mapKey, lockFile, request.write),
                    configFile.writeKeepingUnknownKeys(emptyMap()),
                )
            } catch (e: Exception) {
                listOf(PackMetaSettingError(e.message ?: "Could not apply the change."))
            }
        }

        val newLockFile = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val newConfigFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }
        publish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile)))
        finish(writeErrors)
    }

    // Keyed on the queue head: later writes wait until this one is on disk and reloaded.
    LaunchedEffect(model.modpack.pendingMetaWrites.firstOrNull()) {
        val request    = model.modpack.pendingMetaWrites.firstOrNull() ?: return@LaunchedEffect
        val lockFile   = model.modpack.lockFile?.get()
        val configFile = model.modpack.configFile?.get()
        if (lockFile == null || configFile == null) {
            publish(AppMsg.Modpack(ModpackMsg.MetaWriteFailed(fieldKey = request.fieldKey, errors = emptyList())))
            return@LaunchedEffect
        }

        val writeErrors = withContext(Dispatchers.IO) {
            try {
                val writesLock = request.writesLock || request.replaceLock != null
                val config = request.replaceConfig?.invoke(configFile) ?: configFile
                // The loaded lock has pakku.json project overrides folded into its projects.
                // Writing it would copy them into pakku-lock.json, so lock edits start from the raw file.
                val lock = if (writesLock) {
                    val raw = readRawLockFile() ?: error("Could not read ${LockFile.FILE_NAME}.")
                    request.replaceLock?.invoke(raw) ?: raw
                } else {
                    lockFile
                }
                request.mutate(config, lock)
                listOfNotNull(
                    config.writeKeepingUnknownKeys(request.unknownKeys),
                    if (writesLock) lock.write() else null,
                )
            } catch (e: Exception) {
                listOf(PackMetaSettingError(e.message ?: "Could not apply the change."))
            }
        }

        // Reload after a failure too: mutate may have touched the in-memory files.
        val newLockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val newConfigFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile)))
        publish(
            AppMsg.Modpack(
                if (writeErrors.isEmpty()) ModpackMsg.MetaWriteCompleted
                else ModpackMsg.MetaWriteFailed(fieldKey = request.fieldKey, errors = writeErrors),
            ),
        )
    }

    content()
}

private fun readRawLockFile(): LockFile? = runCatching {
    json.decodeFromString(LockFile.serializer(), Path(workingPath, LockFile.FILE_NAME).readText())
}.getOrNull()
