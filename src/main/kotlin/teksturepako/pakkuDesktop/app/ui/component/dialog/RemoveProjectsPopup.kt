/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import teksturepako.pakkuDesktop.app.actions.fingerprint
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.RemoveDialogPhase
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun RemoveProjectsDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val dialog = model.removeDialog
    if (!dialog.visible || model.actionName != null) return
    if (model.selectedProjectKeys.isEmpty()) return

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(dialog.visible, dialog.phase) {
        kotlinx.coroutines.yield()
        runCatching { rootFocus.requestFocus() }
    }

    val totalCount = model.lockFile?.get()?.getAllProjects()?.size ?: model.selectedProjectKeys.size
    val plan = dialog.plan
    val acceptedRoots = plan?.projects?.filter { it.key in dialog.acceptedProjectIds }.orEmpty()
    val acceptedDeps = plan?.orphansFor(dialog.acceptedProjectIds, dialog.acceptedDepIds).orEmpty()
    val removeCount = acceptedRoots.size + acceptedDeps.size
    val shownFingerprints = mutableSetOf<String>()
    val rowFingerprints = plan?.projects
        ?.flatMap { entry ->
            listOfNotNull(entry.warning?.fingerprint()) +
                entry.orphanedChildren.mapNotNull { it.warning?.fingerprint() }
        }
        ?.toSet()
        .orEmpty()
    val topMessages = plan?.messages
        ?.filter { it.fingerprint() !in rowFingerprints }
        ?.distinctBy { it.fingerprint() }
        .orEmpty()

    val onSecondary: () -> Unit = { publish(ModpackMsg.HideRemoveDialog) }

    when (dialog.phase) {
        RemoveDialogPhase.ConfirmAll -> {
            val onCancel = onSecondary
            Dialog(onDismissRequest = onCancel) {
                ContentBox(
                    Modifier
                        .focusRequester(rootFocus)
                        .dialogConfirmCancelKeys(
                            onDismiss = onCancel,
                            // Enter = Cancel (safe default); remove requires an explicit click.
                            onConfirm = onCancel,
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
                        Header("Remove all projects?")
                        Text(
                            "Do you really want to remove all $totalCount projects?",
                            color = JewelTheme.contentColor.copy(alpha = 0.8f),
                        )
                        ReviewDialogFooter(
                            primaryLabel = "Cancel",
                            primaryEnabled = true,
                            onPrimary = onCancel,
                            secondaryLabel = "Remove all",
                            onSecondary = { publish(ModpackMsg.RemoveAllAcknowledged) },
                        )
                    }
                }
            }
        }

        RemoveDialogPhase.Loading,
        RemoveDialogPhase.Ready -> {
            val primaryEnabled = dialog.phase == RemoveDialogPhase.Ready && acceptedRoots.isNotEmpty()
            val onPrimary: () -> Unit = {
                if (primaryEnabled) publish(ModpackMsg.RemoveConfirmRequested)
            }
            val title = when {
                removeCount == 1 -> "Remove ${
                    (acceptedRoots.firstOrNull()?.project ?: acceptedDeps.firstOrNull()?.project)
                        ?.shortDisplayName() ?: "project"
                }"
                removeCount > 1 -> "Remove $removeCount projects"
                else -> "Remove projects"
            }

            Dialog(onDismissRequest = onSecondary) {
                ContentBox(
                    Modifier
                        .focusRequester(rootFocus)
                        .dialogConfirmCancelKeys(
                            onDismiss = onSecondary,
                            onConfirm = if (primaryEnabled) onPrimary else null,
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
                        Header(title)

                        when (dialog.phase) {
                            RemoveDialogPhase.Loading -> {
                                Row(
                                    Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                    Text(
                                        "Resolving…",
                                        color = JewelTheme.contentColor.copy(alpha = 0.65f),
                                    )
                                }
                            }

                            RemoveDialogPhase.Ready -> {
                                topMessages.forEach { ActionErrorContent(it) }

                                if (plan != null && !plan.isEmpty) {
                                    VerticallyScrollableContainer(
                                        Modifier.fillMaxWidth().heightIn(max = 380.dp),
                                    ) {
                                        Column(
                                            Modifier.fillMaxWidth().padding(end = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            plan.projects.forEach { entry ->
                                                val checked = entry.key in dialog.acceptedProjectIds
                                                ReviewRootRowSimple(
                                                    project = entry.project,
                                                    checked = checked,
                                                    onCheckedChange = { on ->
                                                        val next = if (on) {
                                                            dialog.acceptedProjectIds + entry.key
                                                        } else {
                                                            dialog.acceptedProjectIds - entry.key
                                                        }
                                                        publish(ModpackMsg.RemoveRootSelectionChanged(next))
                                                    },
                                                    warning = entry.warning,
                                                    shownFingerprints = shownFingerprints,
                                                    orphanedChildren = entry.orphanedChildren,
                                                    acceptedDepIds = dialog.acceptedDepIds,
                                                    onDepCheckedChange = { key, on ->
                                                        val next = if (on) {
                                                            dialog.acceptedDepIds + key
                                                        } else {
                                                            dialog.acceptedDepIds - key
                                                        }
                                                        publish(ModpackMsg.RemoveDepSelectionChanged(next))
                                                    },
                                                    depSectionLabel = "Dependencies",
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            RemoveDialogPhase.ConfirmAll -> Unit
                        }

                        ReviewDialogFooter(
                            primaryLabel = when (dialog.phase) {
                                RemoveDialogPhase.Loading -> "Remove"
                                RemoveDialogPhase.Ready ->
                                    primaryRemoveLabel(
                                        (acceptedRoots.map { it.project } + acceptedDeps.map { it.project })
                                            .distinctBy { it.uiKey() },
                                    )
                                RemoveDialogPhase.ConfirmAll -> "Remove"
                            },
                            primaryEnabled = primaryEnabled,
                            onPrimary = onPrimary,
                            secondaryLabel = "Cancel",
                            onSecondary = onSecondary,
                        )
                    }
                }
            }
        }
    }
}
