/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.GroupHeader
import teksturepako.pakkuDesktop.app.plugins.LoadedPlugin
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState
import teksturepako.pakkuDesktop.app.plugins.PluginRegistry
import teksturepako.pakkuDesktop.app.plugins.WithPluginAccess
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.PluginTrust
import java.nio.file.Path

/**
 * Settings list of installed plugins. Plugin screens are not drawn here; Pakku Pro opens
 * on the activation route via [PakkuProSlot].
 */
@Composable
fun PluginsHost(
    plugins: List<LoadedPlugin>,
    failures: PersistentList<PluginLoadState.Failed>,
    pluginsRoot: Path,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectableText(summary(plugins.size, failures.size))
        LabeledCode("folder", pluginsRoot.toString())

        if (plugins.isEmpty() && failures.isEmpty()) {
            SelectableText(
                "No plugins in that folder.",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
            )
        }

        if (plugins.isNotEmpty()) {
            GroupHeader("Installed")
            for (plugin in plugins) {
                key(plugin.id) {
                    LoadedPluginRow(plugin)
                }
            }
        }
        if (failures.isNotEmpty()) {
            GroupHeader("Failed")
            for (failure in failures) {
                key(failure.dirName) {
                    PluginFailureRow(failure)
                }
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
private fun LoadedPluginRow(plugin: LoadedPlugin) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectableText(plugin.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            SelectableText(
                if (plugin.trust == PluginTrust.Official) "Loaded · Official" else "Loaded",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
        if (plugin.name != plugin.id.value) {
            LabeledCode("id", plugin.id.value)
        }
        if (plugin.sha256.isNotEmpty()) {
            LabeledCode("SHA-256", plugin.sha256)
        }
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
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SelectableText(
            failure.dirName,
            color = PakkuDesktopConstants.coral,
            fontWeight = FontWeight.Medium,
        )
        LabeledCode("stage", failure.stage.name)
        SelectableText(
            failure.message,
            color = JewelTheme.contentColor.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun LabeledCode(label: String, value: String) {
    val codeBg = JewelTheme.globalColors.borders.disabled.copy(alpha = 0.45f)
    val chipShape = RoundedCornerShape(4.dp)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectableText(
            label,
            color = JewelTheme.contentColor.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(64.dp),
        )
        SelectableText(
            value,
            color = JewelTheme.contentColor.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(chipShape)
                .background(codeBg)
                .border(1.dp, JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f), chipShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        CopyToClipboardButton(text = value, useSimpleTooltip = true)
    }
}

private fun summary(loaded: Int, failed: Int): String = when {
    loaded == 0 && failed == 0 -> "No plugins loaded"
    failed == 0 -> "$loaded loaded"
    loaded == 0 -> "$failed failed"
    else -> "$loaded loaded, $failed failed"
}
