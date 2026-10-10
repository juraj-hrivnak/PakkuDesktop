/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.data.captureRuntimeDebugInfo
import teksturepako.pakkuDesktop.app.data.copyHistory
import teksturepako.pakkuDesktop.app.data.copySummary
import teksturepako.pakkuDesktop.app.data.formatHeapBytes
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg

@Composable
fun SettingsHeapTab(model: AppModel, publish: (AppMsg) -> Unit) {
    val snapshot = model.heapHistogram
    val loading = model.wantsHeapHistogram
    val footprint = model.heapFootprint
    val runtime = remember { captureRuntimeDebugInfo() }
    val muted = JewelTheme.defaultTextStyle.merge(
        TextStyle(color = JewelTheme.contentColor.copy(alpha = 0.65f)),
    )
    val copyAll = buildString {
        append(runtime.copySummary())
        if (footprint != null) {
            append("\n\n")
            append(footprint.copySummary())
        }
        if (snapshot != null && snapshot.error == null) {
            append("\n\n")
            append(snapshot.copySummary())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (footprint != null) {
                BasicText(
                    buildString {
                        append(formatHeapBytes(footprint.chartBytes))
                        if (footprint.rssBytes != null) {
                            append("   ")
                            append(formatHeapBytes(footprint.heapUsedBytes))
                            append(" Java")
                        }
                    },
                    style = muted,
                )
            }
            CopyToClipboardButton(text = copyAll, useSimpleTooltip = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            runtime.displayLines().forEach { line ->
                BasicText(line, style = muted)
            }
        }
        val error = snapshot?.error
        if (error != null) {
            BasicText(
                error,
                style = JewelTheme.defaultTextStyle.merge(
                    TextStyle(color = JewelTheme.contentColor.copy(alpha = 0.85f)),
                ),
            )
        }
        val history = footprint?.history.orEmpty()
        Column(
            Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (history.isNotEmpty()) {
                HeapHistoryChart(history, Modifier.fillMaxWidth())
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DefaultButton(
                    onClick = { publish(AppMsg.HeapHistogramRequested) },
                    enabled = !loading,
                ) {
                    Text(if (loading) "…" else "Refresh")
                }
                if (history.isNotEmpty()) {
                    CopyToClipboardButton(
                        text = footprint?.copyHistory().orEmpty(),
                        useSimpleTooltip = true,
                    )
                }
            }
            if (snapshot != null && snapshot.error == null) {
                HeapClassBarChart(snapshot, Modifier.fillMaxWidth())
            }
        }
    }
}
