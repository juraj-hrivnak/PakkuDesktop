/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.data.HeapHistogram
import teksturepako.pakkuDesktop.app.data.MAX_HEAP_HISTOGRAM_ROWS
import teksturepako.pakkuDesktop.app.data.parseClassHistogram
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import java.lang.management.ManagementFactory
import javax.management.ObjectName

/**
 * Takes a live `gcClassHistogram` when [AppModel.wantsHeapHistogram] is set.
 * The HotSpot command forces a full GC; it runs on [Dispatchers.IO].
 */
val heapHistogramDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.wantsHeapHistogram) {
        if (!model.wantsHeapHistogram) return@LaunchedEffect
        val histogram = withContext(Dispatchers.IO) {
            runCatching { takeClassHistogram() }.getOrElse { error ->
                HeapHistogram(error = error.message ?: error::class.simpleName ?: "Histogram failed")
            }
        }
        publish(AppMsg.HeapHistogramLoaded(histogram))
    }
    content()
}

private fun takeClassHistogram(): HeapHistogram {
    val server = ManagementFactory.getPlatformMBeanServer()
    val name = ObjectName("com.sun.management:type=DiagnosticCommand")
    val raw = server.invoke(
        name,
        "gcClassHistogram",
        arrayOf<Any>(emptyArray<String>()),
        arrayOf("[Ljava.lang.String;"),
    ) as String
    return parseClassHistogram(raw, MAX_HEAP_HISTOGRAM_ROWS)
}
