/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogNotice
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize
import teksturepako.pakkuDesktop.pkui.component.PkUiNoticeKind
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
    val urlFocus = remember { FocusRequester() }

    fun suggestedFolderName(url: String): String {
        val trimmed = url.trim().removeSuffix(".git").substringAfterLast('/').substringAfterLast(':')
        return trimmed.ifBlank { "modpack" }
    }

    val canSubmit = !cloning && urlState.text.isNotBlank() && destParent != null

    fun dismiss() {
        if (!cloning) publish(AppMsg.HideCloneDialog)
    }

    fun submit() {
        if (!canSubmit) return
        val url = urlState.text.toString().trim()
        val name = nameState.text.toString().ifBlank { suggestedFolderName(url) }
        publish(AppMsg.CloneRequested(url = url, destPath = File(destParent, name).absolutePath))
    }

    PkUiDialog(
        onDismiss = ::dismiss,
        title = "Clone repository",
        icon = AllIconsKeys.General.Vcs,
        subtitle = "Clone a Git repository containing a Pakku modpack.",
        size = PkUiDialogSize.Medium,
        onConfirm = ::submit,
        dismissible = !cloning,
        initialFocus = urlFocus,
        footerStart = if (cloning) {
            {
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Text("Cloning…", color = JewelTheme.contentColor.copy(alpha = 0.65f))
            }
        } else null,
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Clone",
                onPrimary = ::submit,
                primaryEnabled = canSubmit,
                onSecondary = ::dismiss,
                secondaryEnabled = !cloning,
            )
        },
    ) {
        LabeledField("Repository URL") {
            TextField(
                state = urlState,
                modifier = Modifier.fillMaxWidth().focusRequester(urlFocus),
                enabled = !cloning,
                placeholder = { Text("https://github.com/org/modpack.git") },
            )
        }

        LabeledField("Directory") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(AllIconsKeys.Nodes.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    destParent ?: "Choose a parent directory…",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    color = JewelTheme.contentColor.copy(alpha = if (destParent == null) 0.55f else 1f),
                )
                OutlinedButton(
                    onClick = { publish(AppMsg.CloneParentPickerRequested) },
                    enabled = !cloning,
                ) { Text("Browse…") }
            }
        }

        LabeledField("Folder name") {
            TextField(
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
                enabled = !cloning,
                placeholder = { Text(suggestedFolderName(urlState.text.toString())) },
            )
        }

        model.cloneStatus?.let {
            PkUiDialogNotice(it, kind = PkUiNoticeKind.Error)
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = JewelTheme.contentColor.copy(alpha = 0.75f))
        content()
    }
}
