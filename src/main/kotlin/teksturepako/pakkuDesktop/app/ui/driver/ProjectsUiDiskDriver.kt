/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

/**
 * Persists Projects-tab list prefs whenever [AppModel.projectsUi] changes after the initial load.
 * Initial values are loaded synchronously in [teksturepako.pakkuDesktop.app.ui.appComponent] init.
 */
val projectsUiDiskDriver: Driver<AppModel, AppMsg> = { _, model, content ->
    var previous by remember { mutableStateOf<ProjectsUiData?>(null) }

    LaunchedEffect(model.projectsUi) {
        val prev = previous
        previous = model.projectsUi
        if (prev == null || prev == model.projectsUi) return@LaunchedEffect
        delay(250)
        withContext(Dispatchers.IO) {
            model.projectsUi.write()
        }
    }

    content()
}
