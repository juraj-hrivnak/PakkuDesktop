/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.gitBusy
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun GitCommitDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    if (!model.gitCommitDialogVisible || model.gitBusy()) return

    val selectedCount = model.git.selectedFiles.size
    if (selectedCount == 0) return

    val textFieldState = rememberTextFieldState(model.git.commitMessage)
    val inputFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }
    val canCommit = model.git.commitMessage.isNotBlank()

    LaunchedEffect(model.gitCommitDialogVisible) {
        kotlinx.coroutines.yield()
        runCatching { inputFocus.requestFocus() }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { publish(ModpackMsg.GitCommitMessageChanged(it)) }
    }

    val onDismiss = { publish(ModpackMsg.HideGitCommitDialog) }
    val onConfirm = {
        if (canCommit) publish(ModpackMsg.GitCommitRequested)
    }
    val primaryLabel = when (selectedCount) {
        1 -> "Commit 1 file"
        else -> "Commit $selectedCount files"
    }

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .focusRequester(rootFocus)
                .dialogConfirmCancelKeys(
                    onDismiss = onDismiss,
                    onConfirm = if (canCommit) onConfirm else null,
                )
                .widthIn(min = 400.dp, max = 580.dp)
                .animateContentSize(),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Commit")
                Text(
                    when (selectedCount) {
                        1 -> "Commit the selected file with a message."
                        else -> "Commit the $selectedCount selected files with a message."
                    },
                    color = JewelTheme.contentColor.copy(alpha = 0.65f),
                )
                TextField(
                    textFieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 100.dp)
                        .focusRequester(inputFocus),
                    placeholder = { Text("Commit message") },
                )
                ReviewDialogFooter(
                    primaryLabel = primaryLabel,
                    primaryEnabled = canCommit,
                    onPrimary = onConfirm,
                    secondaryLabel = "Cancel",
                    onSecondary = onDismiss,
                )
            }
        }
    }
}
