/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef
import teksturepako.pakkuDesktop.pkui.component.toast.ToastBody
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

/** Toast payloads shown by the app; rendered by [AppToastContent]. */
sealed interface AppToast : ToastBody {
    data class Message(val text: String) : AppToast

    /** [error] must already be [detached]. */
    data class Error(val error: ActionError, val profileLabel: String? = null) : AppToast

    data class FetchSummary(
        val fetched: Int,
        val overrides: List<String>,
        val shelved: List<String>,
        val deleted: List<String>,
    ) : AppToast

    data class Exported(
        val profileName: String,
        val filePath: String,
        val fileSize: String,
        val duration: String,
        val directory: String,
    ) : AppToast

    data class Added(val lines: List<AddedProjectLine>) : AppToast

    data class Removed(val projects: List<Project>) : AppToast
}

/** Error toast with GUI [ActionErrorContent] (project logos, mapped messages). */
internal fun actionErrorToast(error: ActionError) = ToastData(body = AppToast.Error(error.detached()))

internal fun actionErrorToast(message: String) = ToastData(body = AppToast.Message(message))

internal fun actionErrorToast(profileLabel: String, error: ActionError) =
    ToastData(body = AppToast.Error(error.detached(), profileLabel))

internal fun actionInfoToast(message: String) = ToastData(body = AppToast.Message(message))

internal fun actionFetchSummaryToast(
    fetched: Int,
    overrides: List<String>,
    shelved: List<String>,
    deleted: List<String>,
) = ToastData(body = AppToast.FetchSummary(fetched, overrides, shelved, deleted))

internal fun actionExportedToast(
    profileName: String,
    filePath: String,
    fileSize: String,
    duration: String,
    directory: String,
) = ToastData(body = AppToast.Exported(profileName, filePath, fileSize, duration, directory))

/** One added root (optional replace) for the summary toast. */
data class AddedProjectLine(
    val project: Project,
    val replacing: Project? = null,
)

/** Modal-style summary: title + [ProjectRef] rows (same visual language as add/remove review). */
internal fun actionAddedToast(lines: List<AddedProjectLine>) = ToastData(body = AppToast.Added(lines))

internal fun actionRemovedToast(projects: List<Project>) = ToastData(body = AppToast.Removed(projects))

// -- Rendering --

/** Content slot for [teksturepako.pakkuDesktop.pkui.component.toast.ToastHost]. */
@Composable
fun AppToastContent(body: ToastBody) {
    when (body) {
        is AppToast.Message      -> Box(Modifier.padding(16.dp).width(300.dp)) { Text(body.text) }
        is AppToast.Error        -> ErrorToast(body)
        is AppToast.FetchSummary -> FetchSummaryToast(body)
        is AppToast.Exported     -> ExportedToast(body)
        is AppToast.Added        -> AddedToast(body.lines)
        is AppToast.Removed      -> RemovedToast(body.projects)
    }
}

@Composable
private fun ErrorToast(toast: AppToast.Error) {
    Box(Modifier.padding(16.dp).width(320.dp)) {
        if (toast.profileLabel == null) {
            ActionErrorContent(toast.error)
        } else {
            Column {
                Text(toast.profileLabel, fontWeight = FontWeight.Bold)
                ActionErrorContent(toast.error)
            }
        }
    }
}

@Composable
private fun FetchSummaryToast(toast: AppToast.FetchSummary) {
    val (fetched, overrides, shelved, deleted) = toast
    val lines = buildList {
        when (fetched) {
            0 -> if (overrides.isEmpty() && shelved.isEmpty() && deleted.isEmpty()) {
                add("Nothing new to fetch.")
            }
            1 -> add("Fetched 1 file")
            else -> add("Fetched $fetched files")
        }
        when (overrides.size) {
            0 -> Unit
            1 -> add("Synced 1 manual override")
            else -> add("Synced ${overrides.size} manual overrides")
        }
        overrides.take(6).forEach { add("  $it") }
        if (overrides.size > 6) add("  +${overrides.size - 6} more")
        when (shelved.size) {
            0 -> Unit
            1 -> add("Shelved 1 old file")
            else -> add("Shelved ${shelved.size} old files")
        }
        shelved.take(6).forEach { add("  $it") }
        if (shelved.size > 6) add("  +${shelved.size - 6} more")
        when (deleted.size) {
            0 -> Unit
            1 -> add("Deleted 1 old file")
            else -> add("Deleted ${deleted.size} old files")
        }
        deleted.take(6).forEach { add("  $it") }
        if (deleted.size > 6) add("  +${deleted.size - 6} more")
    }
    Column(
        Modifier
            .padding(16.dp)
            .widthIn(min = 240.dp, max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                fontWeight = if (index == 0 || !line.startsWith("  ")) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = if (line.startsWith("  ")) 12.sp else 14.sp,
                color = if (line.startsWith("  ")) {
                    JewelTheme.contentColor.copy(alpha = 0.75f)
                } else {
                    JewelTheme.contentColor
                },
            )
        }
    }
}

@Composable
private fun ExportedToast(toast: AppToast.Exported) {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Column {
            Text("[${toast.profileName} profile]", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("exported to '${toast.filePath}'", style = JewelTheme.consoleTextStyle)
            Text(" (${toast.fileSize}) in ${toast.duration}")
            Spacer(Modifier.height(8.dp))
            DefaultButton(onClick = {
                FileKit.openFileWithDefaultApplication(PlatformFile(toast.directory))
            }) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun AddedToast(lines: List<AddedProjectLine>) {
    if (lines.isEmpty()) return
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
}

@Composable
private fun RemovedToast(projects: List<Project>) {
    if (projects.isEmpty()) return
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
}
