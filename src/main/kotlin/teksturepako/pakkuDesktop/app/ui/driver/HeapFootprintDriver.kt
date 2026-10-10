/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.data.HeapFootprint
import teksturepako.pakkuDesktop.app.data.currentHeapUsedBytes
import teksturepako.pakkuDesktop.app.data.currentProcessRssBytes
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsTab
import teksturepako.pakkuDesktop.elm.Driver

private const val HEAP_FOOTPRINT_FOREGROUND_MS = 1_000L
private const val HEAP_FOOTPRINT_BACKGROUND_MS = 5_000L

/** Polls process RSS and heap used for the life of the app. Does not force GC. */
val heapFootprintDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val watching = model.showSettings && model.settingsTab == SettingsTab.Heap
    val latestAction by rememberUpdatedState(model.modpack.actionName)
    LaunchedEffect(watching) {
        while (isActive) {
            val footprint = withContext(Dispatchers.IO) {
                HeapFootprint(
                    rssBytes = currentProcessRssBytes(),
                    heapUsedBytes = currentHeapUsedBytes(),
                    epochMs = System.currentTimeMillis(),
                )
            }
            publish(AppMsg.HeapFootprintSampled(footprint, latestAction))
            delay(if (watching) HEAP_FOOTPRINT_FOREGROUND_MS else HEAP_FOOTPRINT_BACKGROUND_MS)
        }
    }
    content()
}
