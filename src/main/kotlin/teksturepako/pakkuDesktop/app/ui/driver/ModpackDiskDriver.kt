/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// modpackDiskDriver — loads LockFile + ConfigFile when on Modpack screen
// ---------------------------------------------------------------------------

val modpackDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.screen, model.profile.data.currentProfile) {
        if (model.screen != AppScreen.Modpack) return@LaunchedEffect

        val lockFile   = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val configFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }

        publish(AppMsg.Modpack.Loaded(lockFile, configFile))
    }

    content()
}

