/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

/** Error toast with GUI [ActionErrorContent] (project logos, mapped messages). */
internal fun actionErrorToast(error: ActionError) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(320.dp)) {
        ActionErrorContent(error)
    }
})

internal fun actionErrorToast(message: String) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Text(message)
    }
})

internal fun actionErrorToast(profileLabel: String, error: ActionError) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(320.dp)) {
        Column {
            Text(profileLabel, fontWeight = FontWeight.Bold)
            ActionErrorContent(error)
        }
    }
})

internal fun actionInfoToast(message: String) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Text(message)
    }
})

/** One added root (optional replace) for the summary toast. */
data class AddedProjectLine(
    val project: Project,
    val replacing: Project? = null,
)

/**
 * Modal-style summary: title + [ProjectRef] rows (same visual language as add/remove review).
 */
internal fun actionAddedToast(lines: List<AddedProjectLine>) = ToastData(content = {
    if (lines.isEmpty()) return@ToastData
    Column(
        Modifier
            .padding(16.dp)
            .widthIn(min = 240.dp, max = 340.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when {
                lines.size == 1 && lines.single().replacing != null -> "Replaced"
                lines.size == 1 -> "Added"
                else -> "Added ${lines.size} projects"
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        lines.forEach { line ->
            if (line.replacing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProjectRef(line.replacing, fontSize = 12.sp, iconSize = 14.dp)
                    Text("→", color = JewelTheme.contentColor.copy(alpha = 0.45f), fontSize = 12.sp)
                    ProjectRef(line.project, fontSize = 12.sp, iconSize = 14.dp)
                }
            } else {
                ProjectRef(line.project, fontSize = 12.sp, iconSize = 14.dp)
            }
        }
    }
})

internal fun actionRemovedToast(projects: List<Project>) = ToastData(content = {
    if (projects.isEmpty()) return@ToastData
    Column(
        Modifier
            .padding(16.dp)
            .widthIn(min = 240.dp, max = 340.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when (projects.size) {
                1 -> "Removed"
                else -> "Removed ${projects.size} projects"
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        projects.forEach { project ->
            ProjectRef(project, fontSize = 12.sp, iconSize = 14.dp)
        }
    }
})
