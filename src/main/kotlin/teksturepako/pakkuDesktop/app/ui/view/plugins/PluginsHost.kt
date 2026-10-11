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
import teksturepako.pakkuDesktop.app.plugins.PluginRegistry
import teksturepako.pakkuDesktop.app.plugins.WithPluginAccess
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
                if (plugin.id == PAKKU_PRO) {
                    SelectableText("${plugin.name} — loaded; opens on the activation screen")
                } else {
                    PluginSlot(plugin, states[plugin.id], publish)
                }
            }
        }
        for (failure in failures) {
            key(failure.dirName) {
                PluginFailureRow(failure)
            }
        }
    }
}

/**
 * Hard-coded in the host: the id is the allowlist key, and the manifest is not trusted to
 * pick where a plugin mounts. [PakkuProSlot] is the only place this plugin's view is called.
 */
private val PAKKU_PRO = PluginId("pakku-pro")

/** The pakku-pro plugin view, or its load failure. Mounted by the activation route only. */
@Composable
fun PakkuProSlot(
    plugins: PluginRegistry,
    states: PersistentMap<PluginId, Any?>,
    failures: PersistentList<PluginLoadState.Failed>,
    publish: (AppMsg) -> Unit,
) {
    val plugin = plugins.loaded[PAKKU_PRO]
    val failure = failures.firstOrNull { it.dirName == PAKKU_PRO.value }
    when {
        plugin != null -> PluginSlot(plugin, states[PAKKU_PRO], publish)
        failure != null -> PluginFailureRow(failure)
        else -> SelectableText(
            "The Pakku Pro plugin is not installed.",
            color = JewelTheme.contentColor.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun PluginSlot(plugin: LoadedPlugin, state: Any?, publish: (AppMsg) -> Unit) {
    RecomposeProbe("plugin:${plugin.id.value}")
    val id = plugin.id
    WithPluginAccess(plugin.trust) {
        plugin.erased.view({ publish(AppMsg.Plugin(id, it)) }, state)
    }
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
