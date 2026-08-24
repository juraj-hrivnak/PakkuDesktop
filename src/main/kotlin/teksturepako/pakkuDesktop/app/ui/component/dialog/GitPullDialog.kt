/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.gitBusy
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun GitPullDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    if (!model.gitPullDialogVisible || model.gitBusy()) return

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(model.gitPullDialogVisible) {
        kotlinx.coroutines.yield()
        runCatching { rootFocus.requestFocus() }
    }

    val onDismiss = { publish(ModpackMsg.HideGitPullDialog) }
    val onConfirm = { publish(ModpackMsg.GitPullRequested) }

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .focusRequester(rootFocus)
                .dialogConfirmCancelKeys(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                )
                .widthIn(min = 360.dp, max = 480.dp)
                .animateContentSize(),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Pull")
                Text(
                    "Fetch and merge the latest commits from the remote.",
                    color = JewelTheme.contentColor.copy(alpha = 0.8f),
                )
                ReviewDialogFooter(
                    primaryLabel = "Pull",
                    primaryEnabled = true,
                    onPrimary = onConfirm,
                    secondaryLabel = "Cancel",
                    onSecondary = onDismiss,
                )
            }
        }
    }
}
