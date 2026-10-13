/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.actions.fingerprint
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.RemoveDialogPhase
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

@Composable
fun RemoveProjectsDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val dialog = model.removeDialog
    if (!dialog.visible || model.actionName != null) return
    if (model.selectedProjectKeys.isEmpty()) return

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
    val confirmAll = dialog.phase == RemoveDialogPhase.ConfirmAll
    val primaryEnabled = dialog.phase == RemoveDialogPhase.Ready && acceptedRoots.isNotEmpty()
    val onPrimary: () -> Unit = {
        if (primaryEnabled) publish(ModpackMsg.RemoveConfirmRequested)
    }
    val title = when {
        confirmAll -> "Remove all projects?"
        removeCount == 1 -> "Remove ${
            (acceptedRoots.firstOrNull()?.project ?: acceptedDeps.firstOrNull()?.project)
                ?.shortDisplayName() ?: "project"
        }"
        removeCount > 1 -> "Remove $removeCount projects"
        else -> "Remove projects"
    }

    PkUiDialog(
        onDismiss = onSecondary,
        title = title,
        icon = if (confirmAll) AllIconsKeys.General.WarningDialog else AllIconsKeys.General.Delete,
        subtitle = if (dialog.phase == RemoveDialogPhase.Ready && acceptedDeps.isNotEmpty()) {
            "Includes ${acceptedDeps.size} unused ${if (acceptedDeps.size == 1) "dependency" else "dependencies"}"
        } else null,
        size = PkUiDialogSize.Medium,
        // Wipe-all confirm: Enter = Cancel (safe default); remove requires an explicit click.
        onConfirm = if (confirmAll) onSecondary else if (primaryEnabled) onPrimary else null,
        focusKey = dialog.phase,
        footer = {
            if (confirmAll) {
                OutlinedButton(onClick = { publish(ModpackMsg.RemoveAllAcknowledged) }) {
                    Text("Remove all")
                }
                DefaultButton(onClick = onSecondary) { Text("Cancel") }
            } else {
                PkUiDialogButtons(
                    primaryLabel = if (dialog.phase == RemoveDialogPhase.Ready) {
                        primaryRemoveLabel(
                            (acceptedRoots.map { it.project } + acceptedDeps.map { it.project })
                                .distinctBy { it.uiKey() },
                        )
                    } else "Remove",
                    onPrimary = onPrimary,
                    primaryEnabled = primaryEnabled,
                    onSecondary = onSecondary,
                )
            }
        },
    ) {
        when (dialog.phase) {
            RemoveDialogPhase.ConfirmAll -> Text(
                "Every project in the pack is selected. Do you really want to remove all $totalCount projects?",
                color = JewelTheme.contentColor.copy(alpha = 0.8f),
            )

            RemoveDialogPhase.Loading -> Row(
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

            RemoveDialogPhase.Ready -> {
                topMessages.forEach { ActionErrorContent(it) }

                if (plan != null && !plan.isEmpty) {
                    Column(
                        Modifier.fillMaxWidth(),
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
    }
}
