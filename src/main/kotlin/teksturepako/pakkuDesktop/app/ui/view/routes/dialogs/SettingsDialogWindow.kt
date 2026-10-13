/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.plugins.PluginRegistry
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.component.settings.SettingsHeapTab
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsCredentials
import teksturepako.pakkuDesktop.app.ui.model.SettingsTab
import teksturepako.pakkuDesktop.app.ui.view.plugins.PluginsHost
import java.nio.file.Path
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize
import teksturepako.pakkuDesktop.pkui.component.rememberListComboBoxState

/**
 * Settings as a declarative dialog — same shell as [teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog].
 * Credentials load/save are driven by [teksturepako.pakkuDesktop.app.ui.driver.credentialsDriver].
 */
@Composable
fun SettingsDialog(
    model: AppModel,
    publish: (AppMsg) -> Unit,
    plugins: PluginRegistry,
    pluginsRoot: Path,
) {
    val saving = model.internal.pendingCredentialsUpdate != null
    val uiScale = ProfileData.coerceUiScale(model.profile.data.uiScale)
    val scaleIndex = uiScalePresetIndex(uiScale)
    // Dialog is a separate window at OS density. Keep its content there too so changing
    // scale does not remount the combo or mismatch window size vs layout.
    val parentDensity = LocalDensity.current
    val dialogDensity = remember(parentDensity.density, parentDensity.fontScale, uiScale) {
        Density(
            density = parentDensity.density / uiScale,
            fontScale = parentDensity.fontScale,
        )
    }

    val hide = { publish(AppMsg.HideSettings) }

    CompositionLocalProvider(LocalDensity provides dialogDensity) {
        PkUiDialog(
            onDismiss = hide,
            title = "Settings",
            icon = AllIconsKeys.General.Settings,
            size = when (model.settingsTab) {
                SettingsTab.Heap -> PkUiDialogSize.Large
                else -> PkUiDialogSize.Medium
            },
            scrollable = false,
            footer = {
                DefaultButton(onClick = hide) { Text("Close") }
            },
        ) {
            SettingsTabRow(model.settingsTab, publish)

            VerticallyScrollableContainer(
                Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (model.settingsTab) {
                        SettingsTab.General -> SettingsGeneralTab(
                            model = model,
                            publish = publish,
                            saving = saving,
                            uiScale = uiScale,
                            scaleIndex = scaleIndex,
                        )
                        SettingsTab.Heap -> SettingsHeapTab(model = model, publish = publish)
                        SettingsTab.Plugins -> PluginsHost(
                            plugins = plugins.loadedList,
                            failures = model.pluginFailures,
                            pluginsRoot = pluginsRoot,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabRow(selected: SettingsTab, publish: (AppMsg) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsTab.entries.forEach { tab ->
            val label = when (tab) {
                SettingsTab.General -> "General"
                SettingsTab.Heap -> "Heap"
                SettingsTab.Plugins -> "Plugins"
            }
            if (selected == tab) {
                DefaultButton(onClick = { }) { Text(label) }
            } else {
                OutlinedButton(onClick = { publish(AppMsg.SettingsTabSelected(tab)) }) { Text(label) }
            }
        }
    }
}

@Composable
private fun SettingsGeneralTab(
    model: AppModel,
    publish: (AppMsg) -> Unit,
    saving: Boolean,
    uiScale: Float,
    scaleIndex: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupHeader("Appearance")
        ThemeButton(publish, model.profile.data.intUiTheme)
        Text(
            when (model.profile.data.intUiTheme) {
                IntUiThemes.Dark -> "Dark theme (click icon to switch)"
                IntUiThemes.Light -> "Light theme (click icon to switch)"
                else -> "Theme"
            },
            color = JewelTheme.contentColor.copy(alpha = 0.65f),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("UI scale", color = JewelTheme.contentColor.copy(alpha = 0.75f))
            ListComboBox(
                items = uiScaleLabels,
                selectedIndex = scaleIndex,
                onSelectedItemChange = { index ->
                    if (index == scaleIndex) return@ListComboBox
                    val scale = ProfileData.UI_SCALE_PRESETS.getOrNull(index) ?: return@ListComboBox
                    if (scale == uiScale) return@ListComboBox
                    publish(AppMsg.UiScaleChangeRequested(scale))
                },
                modifier = Modifier.fillMaxWidth(),
                listState = rememberListComboBoxState(uiScaleLabels, scaleIndex),
            )
        }

        GroupHeader("Credentials")
        Text(
            "Stored in ~/.pakku/credentials",
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
        )

        val creds = model.internal.settingsCredentials
        if (creds == null) {
            Text("Loading…", color = JewelTheme.contentColor.copy(alpha = 0.65f))
        } else {
            key(creds) {
                SettingsCredentialsForm(
                    creds = creds,
                    saving = saving,
                    status = model.credentialsStatus,
                    onSave = { cf, gh ->
                        publish(
                            AppMsg.Internal(
                                InternalMsg.CredentialsUpdateRequested(
                                    curseForgeApiKey = cf,
                                    gitHubAccessToken = gh,
                                ),
                            ),
                        )
                    },
                )
            }
        }
    }
}

private val uiScaleLabels = ProfileData.UI_SCALE_PRESETS.map { scale ->
    "${(scale * 100).toInt()}%"
}

private fun uiScalePresetIndex(uiScale: Float): Int {
    val coerced = ProfileData.coerceUiScale(uiScale)
    return ProfileData.UI_SCALE_PRESETS.indices.minBy { index ->
        kotlin.math.abs(ProfileData.UI_SCALE_PRESETS[index] - coerced)
    }
}

@Composable
private fun SettingsCredentialsForm(
    creds: SettingsCredentials,
    saving: Boolean,
    status: String?,
    onSave: (curseForgeApiKey: String, gitHubAccessToken: String) -> Unit,
) {
    val cfState = rememberTextFieldState(creds.curseForgeApiKey)
    val ghState = rememberTextFieldState(creds.gitHubAccessToken)

    SettingsLabeledField("CurseForge API key", cfState, enabled = !saving)
    SettingsLabeledField("GitHub access token", ghState, enabled = !saving)

    status?.let {
        Text(it, color = JewelTheme.contentColor.copy(alpha = 0.8f))
    }

    DefaultButton(
        onClick = {
            onSave(cfState.text.toString(), ghState.text.toString())
        },
        enabled = !saving,
    ) {
        Text(if (saving) "Saving…" else "Save credentials")
    }
}

@Composable
private fun SettingsLabeledField(
    label: String,
    state: TextFieldState,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = JewelTheme.contentColor.copy(alpha = 0.75f))
        TextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}
