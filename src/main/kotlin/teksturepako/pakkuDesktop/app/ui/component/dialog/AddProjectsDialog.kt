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
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import teksturepako.pakkuDesktop.app.actions.fingerprint
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.AddDialogPhase
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun AddProjectsDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val dialog = model.addDialog
    if (!dialog.visible || model.actionName != null) return

    val textFieldState = rememberTextFieldState(dialog.query)
    val inputFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(dialog.visible, dialog.phase) {
        when (dialog.phase) {
            AddDialogPhase.Input -> {
                kotlinx.coroutines.yield()
                runCatching { inputFocus.requestFocus() }
            }
            else -> {
                kotlinx.coroutines.yield()
                runCatching { rootFocus.requestFocus() }
            }
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { publish(ModpackMsg.AddQueryChanged(it)) }
    }

    LaunchedEffect(dialog.visible) {
        if (dialog.visible && dialog.phase == AddDialogPhase.Input && dialog.query.isEmpty()) {
            textFieldState.edit {
                replace(0, length, "")
            }
        }
    }

    val plan = dialog.plan
    val acceptedEntries = plan?.entries?.filter { it.key in dialog.acceptedRootIds }.orEmpty()
    val shownFingerprints = mutableSetOf<String>()
    val entryFingerprints = plan?.entries
        ?.flatMap { it.warnings.map { w -> w.fingerprint() } }
        ?.toSet()
        .orEmpty()
    val topMessages = plan?.messages
        ?.filter { it.fingerprint() !in entryFingerprints }
        ?.distinctBy { it.fingerprint() }
        .orEmpty()

    val emptyReview = plan == null || plan.isEmpty
    val primaryEnabled = when (dialog.phase) {
        AddDialogPhase.Input -> dialog.query.isNotBlank()
        AddDialogPhase.Resolving -> false
        AddDialogPhase.Review -> if (emptyReview) true else acceptedEntries.isNotEmpty()
    }

    val onPrimary: () -> Unit = {
        when (dialog.phase) {
            AddDialogPhase.Input -> publish(ModpackMsg.AddResolveRequested)
            AddDialogPhase.Resolving -> Unit
            AddDialogPhase.Review -> {
                if (emptyReview) publish(ModpackMsg.AddBackToInput)
                else publish(ModpackMsg.AddConfirmRequested)
            }
        }
    }

    val onSecondary: () -> Unit = {
        when (dialog.phase) {
            AddDialogPhase.Input, AddDialogPhase.Resolving -> publish(ModpackMsg.HideAddDialog)
            AddDialogPhase.Review -> {
                if (emptyReview) publish(ModpackMsg.HideAddDialog)
                else publish(ModpackMsg.AddBackToInput)
            }
        }
    }

    val (primaryLabel, secondaryLabel) = when (dialog.phase) {
        AddDialogPhase.Input -> "Add" to "Cancel"
        AddDialogPhase.Resolving -> "Add" to "Cancel"
        AddDialogPhase.Review -> if (emptyReview) {
            "Back" to "Cancel"
        } else {
            primaryAddLabel(acceptedEntries.map { it.project }) to "Back"
        }
    }

    Dialog(onDismissRequest = { publish(ModpackMsg.HideAddDialog) }) {
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
                Header("Add projects")

                when (dialog.phase) {
                    AddDialogPhase.Input -> {
                        Text(
                            "Project names, slugs, or URLs — comma-separated.",
                            color = JewelTheme.contentColor.copy(alpha = 0.65f),
                        )
                        TextField(
                            state = textFieldState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocus),
                            placeholder = { Text("sodium, iris, gh:owner/repo") },
                        )
                    }

                    AddDialogPhase.Resolving -> {
                        Text(
                            dialog.resolveStatus ?: "Resolving…",
                            color = JewelTheme.contentColor.copy(alpha = 0.75f),
                        )
                    }

                    AddDialogPhase.Review -> {
                        topMessages.forEach { ActionErrorContent(it) }

                        if (!emptyReview && plan != null) {
                            VerticallyScrollableContainer(
                                Modifier.fillMaxWidth().heightIn(max = 380.dp),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    plan.entries.forEach { entry ->
                                        val checked = entry.key in dialog.acceptedRootIds
                                        ReviewRootRow(
                                            project = entry.project,
                                            checked = checked,
                                            onCheckedChange = { on ->
                                                val next = if (on) {
                                                    dialog.acceptedRootIds + entry.key
                                                } else {
                                                    dialog.acceptedRootIds - entry.key
                                                }
                                                publish(ModpackMsg.AddRootSelectionChanged(next))
                                            },
                                            replacing = entry.replacing,
                                            warnings = entry.warnings,
                                            shownFingerprints = shownFingerprints,
                                            deps = entry.deps,
                                            depSectionLabel = "Also adds",
                                            depCue = "new",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ReviewDialogFooter(
                    primaryLabel = primaryLabel,
                    primaryEnabled = primaryEnabled,
                    onPrimary = onPrimary,
                    secondaryLabel = secondaryLabel,
                    onSecondary = onSecondary,
                )
            }
        }
    }
}
