/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab

/** Thin status strip: totals / update counts on the left, selection on the right. */
@Composable
fun ListActions(publish: (ModpackMsg) -> Unit, model: ProjectsTabModel) {
    val totalCount = model.lockFile?.get()?.getAllProjects()?.size ?: 0
    val selectedCount = model.selectedProjectKeys.size
    val pendingUpdateCount = model.updatePreviews?.count { !it.value.applied }
    val syncCount = model.syncPendingCount()
    val busy = model.actionName != null
    val checking = busy && model.actionName == "Checking updates"

    val selectionLabel = when {
        selectedCount == 0 -> "0 of $totalCount selected"
        selectedCount == totalCount && totalCount > 0 -> "All $selectedCount of $totalCount selected"
        else -> "$selectedCount of $totalCount selected"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$totalCount total",
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
            fontSize = 12.sp,
        )

        when {
            checking -> {
                Text(
                    text = "Checking…",
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                )
            }
            pendingUpdateCount != null -> {
                val updatesLabel = when (pendingUpdateCount) {
                    0 -> "up to date"
                    1 -> "1 update"
                    else -> "$pendingUpdateCount updates"
                }
                if (pendingUpdateCount > 0) {
                    Link(
                        text = updatesLabel,
                        onClick = {
                            publish(ModpackMsg.FilterUpdatesOnlyChanged(!model.filterUpdatesOnly))
                        },
                        enabled = !busy,
                    )
                } else {
                    Text(
                        text = updatesLabel,
                        color = JewelTheme.contentColor.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (syncCount > 0) {
            Link(
                text = if (syncCount == 1) "1 to sync" else "$syncCount to sync",
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.FILES)) },
                enabled = !busy,
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = selectionLabel,
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}

@Composable
fun FilesFooterStatus(model: FilesTabModel, links: List<FilesLink>) {
    val selectedCount = model.selectedProjectKeys.size
    val busy = model.actionName != null
    val scanning = busy && (model.actionName == "Scanning files" || model.actionName == "Detecting files")

    val selectionLabel = when {
        selectedCount == 0 -> "0 selected"
        else -> "$selectedCount selected"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                links.size == 1 -> "1 file"
                else -> "${links.size} files"
            },
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
            fontSize = 12.sp,
        )

        when {
            scanning -> {
                Text(
                    text = "Scanning…",
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                )
            }
            else -> {
                Text(
                    text = filesFooterChangeSummary(links, model.filesPreviewMode),
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = selectionLabel,
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}
