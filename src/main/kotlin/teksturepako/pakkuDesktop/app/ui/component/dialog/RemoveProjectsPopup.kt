/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.RemovalEntry
import teksturepako.pakkuDesktop.app.actions.RemovalPlan
import teksturepako.pakkuDesktop.app.actions.buildRemovalPlan
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

private sealed interface RemoveStep {
    data object Loading : RemoveStep
    data class Ready(val plan: RemovalPlan) : RemoveStep
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoveProjectsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    lockFile: LockFile,
    projects: List<Project>,
    onConfirm: (RemovalPlan) -> Unit,
) {
    if (!visible) return

    var step by remember { mutableStateOf<RemoveStep>(RemoveStep.Loading) }
    var acceptedProjects by remember { mutableStateOf<Set<String>>(emptySet()) }
    var acceptedDeps by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(projects.mapNotNull { it.pakkuId }.toSet()) {
        step = RemoveStep.Loading
        val plan = withContext(Dispatchers.IO) { buildRemovalPlan(lockFile, projects) }
        acceptedProjects = plan.projects
            .filter { it.isRecommended }
            .mapNotNull { it.project.pakkuId }
            .toSet()
        acceptedDeps = plan.orphanedDeps
            .filter { it.isRecommended }
            .mapNotNull { it.project.pakkuId }
            .toSet()
        step = RemoveStep.Ready(plan)
    }

    fun confirm() {
        val ready = step as? RemoveStep.Ready ?: return
        val filtered = RemovalPlan(
            projects = ready.plan.projects.filter { it.project.pakkuId in acceptedProjects },
            orphanedDeps = ready.plan.orphanedDeps.filter { it.project.pakkuId in acceptedDeps },
            messages = ready.plan.messages,
        )
        if (filtered.isEmpty) return
        onConfirm(filtered)
        onDismiss()
    }

    val removeCount = acceptedProjects.size + acceptedDeps.size

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .dialogConfirmCancelKeys(onDismiss = onDismiss, onConfirm = { confirm() })
                .widthIn(min = 360.dp, max = 520.dp),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Remove projects")

                when (val s = step) {
                    RemoveStep.Loading -> {
                        Text("Resolving…", color = JewelTheme.contentColor.copy(alpha = 0.65f))
                    }

                    is RemoveStep.Ready -> {
                        val plan = s.plan
                        val muted = JewelTheme.contentColor.copy(alpha = 0.65f)

                        if (plan.projects.isNotEmpty()) {
                            Text(
                                "${plan.projects.size} selected",
                                color = muted,
                            )
                        }

                        plan.messages.forEach { ActionErrorContent(it) }

                        VerticallyScrollableContainer(
                            Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (plan.projects.isNotEmpty()) {
                                    RemovalSection(
                                        title = "Selected",
                                        entries = plan.projects,
                                        accepted = acceptedProjects,
                                        onAcceptedChange = { acceptedProjects = it },
                                    )
                                }
                                if (plan.orphanedDeps.isNotEmpty()) {
                                    RemovalSection(
                                        title = "Unused dependencies",
                                        subtitle = "No longer required by anything else",
                                        entries = plan.orphanedDeps,
                                        accepted = acceptedDeps,
                                        onAcceptedChange = { acceptedDeps = it },
                                    )
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DefaultButton(
                                onClick = { confirm() },
                                enabled = removeCount > 0,
                            ) {
                                Text(
                                    if (removeCount == 1) "Remove 1"
                                    else "Remove $removeCount",
                                )
                            }
                            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovalSection(
    title: String,
    entries: List<RemovalEntry>,
    accepted: Set<String>,
    onAcceptedChange: (Set<String>) -> Unit,
    subtitle: String? = null,
) {
    val muted = JewelTheme.contentColor.copy(alpha = 0.65f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GroupHeader(title)
        subtitle?.let {
            Text(it, color = muted)
        }
        entries.forEach { entry ->
            val id = entry.project.pakkuId ?: return@forEach
            RemovalCheckRow(
                entry = entry,
                checked = id in accepted,
                onCheckedChange = { checked ->
                    onAcceptedChange(
                        if (checked) accepted + id else accepted - id,
                    )
                },
            )
        }
    }
}

@Composable
private fun RemovalCheckRow(
    entry: RemovalEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.padding(4.dp),
            )
            ProjectRef(entry.project, modifier = Modifier.weight(1f, fill = false))
        }
        entry.warning?.let { warning ->
            ActionErrorContent(
                warning,
                compact = true,
                modifier = Modifier.padding(start = 36.dp),
            )
        }
    }
}
