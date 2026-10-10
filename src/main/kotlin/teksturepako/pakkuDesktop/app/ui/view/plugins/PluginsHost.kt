/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.plugins.LoadedPlugin
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.plugin.PluginId
import java.nio.file.Path

@Composable
fun PluginsHost(
    plugins: List<LoadedPlugin>,
    states: PersistentMap<PluginId, Any?>,
    failures: PersistentList<PluginLoadState.Failed>,
    publish: (AppMsg) -> Unit,
    pluginsRoot: Path,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (plugins.isEmpty() && failures.isEmpty()) {
            SelectableText(
                "No plugins loaded. Drop plugin folders in:\n$pluginsRoot",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
            )
        }
        for (plugin in plugins) {
            key(plugin.id) {
                PluginSlot(plugin, states[plugin.id], publish)
            }
        }
        for (failure in failures) {
            key(failure.dirName) {
                PluginFailureRow(failure)
            }
        }
    }
}

@Composable
private fun PluginSlot(plugin: LoadedPlugin, state: Any?, publish: (AppMsg) -> Unit) {
    RecomposeProbe("plugin:${plugin.id.value}")
    val id = plugin.id
    plugin.erased.view({ publish(AppMsg.Plugin(id, it)) }, state)
}

@Composable
private fun PluginFailureRow(failure: PluginLoadState.Failed) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            key = AllIconsKeys.General.ExclMark,
            contentDescription = "Plugin load failure",
            tint = PakkuDesktopConstants.coral,
            modifier = Modifier.size(16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectableText(
                "${failure.dirName} — failed at ${failure.stage}",
                color = PakkuDesktopConstants.coral,
            )
            SelectableText(
                "${failure.message} (see log for details)",
                color = JewelTheme.contentColor.copy(alpha = 0.75f),
            )
        }
    }
}
