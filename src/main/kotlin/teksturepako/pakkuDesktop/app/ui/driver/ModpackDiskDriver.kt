/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readManualOverrides
import teksturepako.pakkuDesktop.app.actions.toPreview
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

// -- modpackDiskDriver --

val modpackDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.screen, model.profile.data.currentProfile) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect

        val lockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val configFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack(ModpackMsg.Loaded(lockFile, configFile)))

        // If the modpack directory has no lock file yet, open the New Modpack dialog.
        if (lockFile.getError() is FileNotFound) {
            publish(AppMsg.Modpack(ModpackMsg.ShowNewModpack))
        }
    }

    LaunchedEffect(
        model.screen,
        model.modpack.lockFile,
        model.modpack.configFile,
        model.modpack.actionName,
    ) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect
        if (model.modpack.actionName != null) return@LaunchedEffect
        val overrides = withContext(Dispatchers.IO) {
            readManualOverrides(model.modpack.configFile?.get()).map { it.toPreview() }
        }
        publish(AppMsg.Modpack(ModpackMsg.ManualOverridesLoaded(overrides)))
    }

    content()
}
