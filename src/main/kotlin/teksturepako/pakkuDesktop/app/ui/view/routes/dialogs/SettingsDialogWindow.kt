/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsCredentials
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

/**
 * Settings as a declarative dialog — same shell as [teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog].
 * Credentials load/save are driven by [teksturepako.pakkuDesktop.app.ui.driver.credentialsDriver].
 */
@Composable
fun SettingsDialog(
    model: AppModel,
    publish: (AppMsg) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val saving = model.pendingCredentialsUpdate != null

    Dialog(onDismissRequest = { publish(AppMsg.HideSettings) }) {
        ContentBox(
            Modifier
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(onDismiss = { publish(AppMsg.HideSettings) })
                .widthIn(min = 360.dp, max = 520.dp),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Settings", Modifier.padding(vertical = 4.dp))

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

                GroupHeader("Credentials")
                Text(
                    "Stored in ~/.pakku/credentials",
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                )

                val creds = model.settingsCredentials
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
                                    AppMsg.CredentialsUpdateRequested(
                                        curseForgeApiKey = cf,
                                        gitHubAccessToken = gh,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
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
