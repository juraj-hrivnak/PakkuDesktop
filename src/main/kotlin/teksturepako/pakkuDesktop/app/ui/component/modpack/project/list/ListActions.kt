/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab

/** Thin status strip: totals / update counts on the left, selection on the right. */
@Composable
fun ListActions(
    publish: (ModpackMsg) -> Unit,
    model: ProjectsTabModel,
    lastClickedIndex: MutableState<Int?>,
) {
    val allProjects = model.lockFile?.get()?.getAllProjects().snapshotList()
    val totalCount = allProjects.size
    val selectedCount = model.selectedProjectKeys.size
    val visibleKeys = model.filteredAndSortedProjects(allProjects).map { it.uiKey() }
    val pendingUpdateCount = model.updatePreviews?.count { !it.value.applied }
    val syncCount = model.syncPendingCount()
    val busy = model.actionName != null
    val checking = busy && model.actionName == "Checking updates"

    FooterRow {
        FooterText("$totalCount total")

        when {
            checking -> FooterText("Checking…")
            pendingUpdateCount != null -> {
                val updatesLabel = when (pendingUpdateCount) {
                    0 -> "up to date"
                    1 -> "1 update"
                    else -> "$pendingUpdateCount updates"
                }
                if (pendingUpdateCount > 0) {
                    FooterLink(
                        text = updatesLabel,
                        onClick = {
                            publish(ModpackMsg.FilterUpdatesOnlyChanged(!model.filterUpdatesOnly))
                        },
                        enabled = !busy,
                    )
                } else {
                    FooterText(updatesLabel)
                }
            }
        }
        if (syncCount > 0) {
            FooterLink(
                text = if (syncCount == 1) "1 to sync" else "$syncCount to sync",
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.FILES)) },
                enabled = !busy,
            )
        }

        Spacer(Modifier.weight(1f))
        SelectionStatus(
            selectedCount = selectedCount,
            totalCount = totalCount,
            canSelectAll = visibleKeys.any { it !in model.selectedProjectKeys },
            onSelectAll = {
                publish(ModpackMsg.ProjectsSelected(visibleKeys.toSet()))
                lastClickedIndex.value = null
            },
            onUnselectAll = {
                publish(ModpackMsg.ProjectsCleared())
                lastClickedIndex.value = null
            },
        )
    }
}

@Composable
fun FilesFooterStatus(
    publish: (ModpackMsg) -> Unit,
    model: FilesTabModel,
    links: List<FilesLink>,
    selectableKeys: List<String>,
    lastClickedIndex: MutableState<Int?>,
) {
    val busy = model.actionName != null
    val scanning = busy && (model.actionName == "Scanning files" || model.actionName == "Detecting files")
    val selectable = model.filesPreviewMode != FilesPreviewMode.None

    FooterRow {
        FooterText(if (links.size == 1) "1 file" else "${links.size} files")
        FooterText(if (scanning) "Scanning…" else filesFooterChangeSummary(links, model.filesPreviewMode))

        Spacer(Modifier.weight(1f))
        if (selectable) {
            SelectionStatus(
                selectedCount = selectableKeys.count { it in model.selectedProjectKeys },
                totalCount = selectableKeys.size,
                canSelectAll = selectableKeys.any { it !in model.selectedProjectKeys },
                onSelectAll = {
                    publish(ModpackMsg.ProjectsSelected(selectableKeys.toSet()))
                    lastClickedIndex.value = null
                },
                onUnselectAll = {
                    publish(ModpackMsg.ProjectsCleared())
                    lastClickedIndex.value = null
                },
            )
        }
    }
}

/** "X of Y selected" followed by Select all / Unselect all; shared by every tab footer. */
@Composable
fun SelectionStatus(
    selectedCount: Int,
    totalCount: Int,
    canSelectAll: Boolean,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                totalCount > 0 && selectedCount == totalCount -> "All $totalCount selected"
                else -> "$selectedCount of $totalCount selected"
            },
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        FooterLink(text = "Select all", onClick = onSelectAll, enabled = canSelectAll)
        FooterLink(text = "Unselect all", onClick = onUnselectAll, enabled = selectedCount > 0)
    }
}

@Composable
fun FooterRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun FooterText(text: String) {
    Text(
        text = text,
        color = JewelTheme.contentColor.copy(alpha = 0.55f),
        fontSize = 12.sp,
    )
}

@Composable
fun FooterLink(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Link(
        text = text,
        onClick = onClick,
        enabled = enabled,
        textStyle = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
    )
}
