/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.actions.fingerprint
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.model.AddDialogPhase
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogNotice
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

@Composable
fun AddProjectsDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val dialog = model.addDialog
    if (!dialog.visible || model.actionName != null) return

    val textFieldState = rememberTextFieldState(dialog.query)
    val inputFocus = remember { FocusRequester() }

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
                else if (!dialog.allowInput) publish(ModpackMsg.HideAddDialog)
                else publish(ModpackMsg.AddBackToInput)
            }
        }
    }

    val (primaryLabel, secondaryLabel) = when (dialog.phase) {
        AddDialogPhase.Input -> "Add" to "Cancel"
        AddDialogPhase.Resolving -> dialog.confirmVerb to "Cancel"
        AddDialogPhase.Review -> if (emptyReview) {
            if (dialog.allowInput) "Back" to "Cancel" else "OK" to "Cancel"
        } else {
            primaryAddLabel(acceptedEntries.map { it.project }, dialog.confirmVerb) to
                if (dialog.allowInput) "Back" else "Cancel"
        }
    }

    val isUpdate = dialog.confirmVerb == "Update"
    val subtitle = when (dialog.phase) {
        AddDialogPhase.Input -> "Search CurseForge, Modrinth, or GitHub."
        AddDialogPhase.Resolving -> null
        AddDialogPhase.Review -> plan?.entries?.size?.takeIf { it > 0 }?.let { count ->
            "${acceptedEntries.size} of $count selected"
        }
    }

    PkUiDialog(
        onDismiss = onSecondary,
        title = "${dialog.confirmVerb} projects",
        icon = if (isUpdate) AllIconsKeys.Actions.CheckOut else AllIconsKeys.General.Add,
        subtitle = subtitle,
        size = PkUiDialogSize.Medium,
        onConfirm = if (primaryEnabled) onPrimary else null,
        initialFocus = if (dialog.phase == AddDialogPhase.Input) inputFocus else null,
        focusKey = dialog.phase,
        footer = {
            PkUiDialogButtons(
                primaryLabel = primaryLabel,
                onPrimary = onPrimary,
                primaryEnabled = primaryEnabled,
                secondaryLabel = secondaryLabel,
                onSecondary = onSecondary,
            )
        },
    ) {
        when (dialog.phase) {
            AddDialogPhase.Input -> {
                Text(
                    "CurseForge/Modrinth slugs, GitHub repos, or file IDs — comma-separated.",
                    color = JewelTheme.contentColor.copy(alpha = 0.65f),
                )
                TextField(
                    state = textFieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(inputFocus),
                    placeholder = { Text("jei, CaffeineMC/sodium, 238222:5101366") },
                )
            }

            AddDialogPhase.Resolving -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    Text(
                        dialog.resolveStatus ?: "Resolving…",
                        color = JewelTheme.contentColor.copy(alpha = 0.75f),
                    )
                }
            }

            AddDialogPhase.Review -> {
                topMessages.forEach { ActionErrorContent(it) }

                if (emptyReview) {
                    if (topMessages.isEmpty()) PkUiDialogNotice("Nothing to ${dialog.confirmVerb.lowercase()}.")
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
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
}
