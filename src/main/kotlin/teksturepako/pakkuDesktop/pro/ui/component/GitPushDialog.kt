/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState

@Composable
fun GitPushDialog(
    gitState: GitState,
    visible: Boolean,
    onDismiss: () -> Unit,
    onPush: () -> Unit,
) {
    if (!visible) return

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(visible) {
        runCatching { focusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(
                    onDismiss = onDismiss,
                    onConfirm = {
                        if (gitState.outgoingCommits.isNotEmpty()) {
                            onPush()
                            onDismiss()
                        }
                    },
                )
                .widthIn(max = 560.dp),
        ) {
            FlowColumn(
                Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Commits not yet on the remote (same as \u201coutgoing\u201d in IntelliJ):",
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                )
                if (gitState.outgoingCommits.isEmpty()) {
                    Text(
                        "Nothing to push — your current branch matches the remote or has no upstream commits.",
                        color = JewelTheme.contentColor,
                    )
                } else {
                    gitState.outgoingCommits.forEach { outgoingCommit ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(outgoingCommit.hash, color = JewelTheme.contentColor.copy(alpha = 0.55f))
                            Text(outgoingCommit.message, color = JewelTheme.contentColor)
                        }
                    }
                }
                Row {
                    DefaultButton(
                        enabled = gitState.outgoingCommits.isNotEmpty(),
                        onClick = {
                            onPush()
                            onDismiss()
                        },
                    ) {
                        Text("Push")
                    }
                }
            }
        }
    }
}
