/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys
import java.io.File

@Composable
fun CloneRepositoryDialog(
    model: AppModel,
    publish: (AppMsg) -> Unit,
) {
    val urlState = rememberTextFieldState()
    val nameState = rememberTextFieldState()
    val cloning = model.pendingClone != null
    val destParent = model.cloneDestParent

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    fun suggestedFolderName(url: String): String {
        val trimmed = url.trim().removeSuffix(".git").substringAfterLast('/').substringAfterLast(':')
        return trimmed.ifBlank { "modpack" }
    }

    fun dismiss() {
        if (!cloning) publish(AppMsg.HideCloneDialog)
    }

    fun submit() {
        val url = urlState.text.toString().trim()
        val parent = destParent
        val name = nameState.text.toString().ifBlank { suggestedFolderName(url) }
        if (url.isBlank() || parent.isNullOrBlank()) {
            // Local validation feedback: reuse cloneStatus channel via update would need a msg;
            // keep it as a soft guard — button stays enabled until both are set.
            return
        }
        val dest = File(parent, name).absolutePath
        publish(AppMsg.CloneRequested(url = url, destPath = dest))
    }

    Dialog(onDismissRequest = { dismiss() }) {
        ContentBox(
            Modifier
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(
                    onDismiss = { dismiss() },
                    onConfirm = { if (!cloning) submit() },
                )
                .widthIn(min = 360.dp, max = 560.dp),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Header("Clone repository", Modifier.padding(vertical = 4.dp))

                Text("Repository URL")
                TextField(
                    state = urlState,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cloning,
                    placeholder = { Text("https://github.com/org/modpack.git") },
                )

                Text("Destination folder")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        destParent ?: "Choose a parent directory…",
                        modifier = Modifier.weight(1f),
                        color = JewelTheme.contentColor.copy(alpha = if (destParent == null) 0.55f else 1f),
                    )
                    OutlinedButton(
                        onClick = { publish(AppMsg.CloneParentPickerRequested) },
                        enabled = !cloning,
                    ) { Text("Browse…") }
                }

                Text("Folder name")
                TextField(
                    state = nameState,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cloning,
                    placeholder = { Text(suggestedFolderName(urlState.text.toString())) },
                )

                model.cloneStatus?.let {
                    Text(it, color = JewelTheme.contentColor.copy(alpha = 0.85f))
                }

                DefaultButton(
                    onClick = { submit() },
                    enabled = !cloning && !urlState.text.isBlank() && destParent != null,
                ) {
                    Text(if (cloning) "Cloning…" else "Clone")
                }
            }
        }
    }
}
