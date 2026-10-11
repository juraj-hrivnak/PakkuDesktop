/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.dialog.ReviewDialogFooter
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.gitBusy
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun GitPushDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    if (!model.gitDropdown.pushDialogVisible || model.gitBusy()) return

    val outgoing = model.git.outgoingCommits
    val canPush = outgoing.isNotEmpty()
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(model.gitDropdown.pushDialogVisible) {
        kotlinx.coroutines.yield()
        runCatching { rootFocus.requestFocus() }
    }

    val onDismiss = { publish(ModpackMsg.GitDropdown(GitDropdownMsg.HidePushDialog)) }
    val onConfirm = {
        if (canPush) publish(ModpackMsg.GitDropdown(GitDropdownMsg.PushRequested))
    }
    val primaryLabel = when (outgoing.size) {
        0 -> "Push"
        1 -> "Push 1 commit"
        else -> "Push ${outgoing.size} commits"
    }

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .focusRequester(rootFocus)
                .dialogConfirmCancelKeys(
                    onDismiss = onDismiss,
                    onConfirm = if (canPush) onConfirm else null,
                )
                .widthIn(min = 400.dp, max = 580.dp)
                .animateContentSize(),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .heightIn(max = 580.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Push")
                Text(
                    "Commits not yet on the remote.",
                    color = JewelTheme.contentColor.copy(alpha = 0.65f),
                )
                if (outgoing.isEmpty()) {
                    Text(
                        "Nothing to push — the current branch matches the remote.",
                        color = JewelTheme.contentColor.copy(alpha = 0.8f),
                    )
                } else {
                    VerticallyScrollableContainer(
                        Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            outgoing.forEach { commit ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(commit.hash, color = JewelTheme.contentColor.copy(alpha = 0.55f))
                                    Text(commit.message, color = JewelTheme.contentColor)
                                }
                            }
                        }
                    }
                }
                ReviewDialogFooter(
                    primaryLabel = primaryLabel,
                    primaryEnabled = canPush,
                    onPrimary = onConfirm,
                    secondaryLabel = "Cancel",
                    onSecondary = onDismiss,
                )
            }
        }
    }
}
