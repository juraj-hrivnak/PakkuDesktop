/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import io.klogging.logger
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.InitSpec
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val initLogger = logger("InitImpl")

/**
 * Creates a minimal pakku-lock.yaml + pakku.toml for an uninitialized directory.
 */
suspend fun initSuspend(
    spec: InitSpec,
    onToast: suspend (ToastData) -> Unit,
) {
    val packName = spec.name.ifBlank { "Modpack" }
    val mcVersion = spec.mcVersion.ifBlank { "1.20.1" }
    val loader = spec.loader.ifBlank { "fabric" }.lowercase()
    val target = spec.target.ifBlank { "modrinth" }.lowercase()

    val lockFile = LockFile()
    lockFile.setMcVersions(listOf(mcVersion))
    lockFile.setLoader(loader, "")
    lockFile.setTarget(target)
    lockFile.write()?.let { error ->
        onToast(actionErrorToast(error))
        return
    }

    val configFile = ConfigFile()
    configFile.setName(packName)
    configFile.setVersion("0.0.1")
    configFile.write()?.let { error ->
        onToast(actionErrorToast(error))
        return
    }

    initLogger.info { "Initialized modpack '$packName' ($mcVersion / $loader / $target)" }
    onToast(actionInfoToast("Created modpack '$packName'."))
}
