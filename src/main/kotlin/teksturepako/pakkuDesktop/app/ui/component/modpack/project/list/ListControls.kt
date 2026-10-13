/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarActionButton
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarSeparator
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

/** Must match the checkbox column in [ListImpl] for pixel alignment. */
internal val ProjectsListCheckboxColumnWidth = 40.dp

@Composable
fun FilesSelectionControls(
    publish: (ModpackMsg) -> Unit,
    model: FilesTabModel,
    lastClickedIndex: MutableState<Int?>,
    selectableKeys: Collection<String>,
    hasFolders: Boolean,
    linksEmpty: Boolean,
    folderIds: Set<String> = emptySet(),
    showSelection: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSelection) {
            Spacer(Modifier.width(3.dp))
            MasterSelectionCheckbox(
                keys = selectableKeys,
                selectedKeys = model.selectedProjectKeys,
                publish = publish,
                lastClickedIndex = lastClickedIndex,
            )
            ToolbarSeparator()
        }
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Expandall,
            label = "Expand all",
            enabled = !linksEmpty && hasFolders,
            onClick = { publish(ModpackMsg.FilesExpandAllFolders) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Collapseall,
            label = "Collapse all",
            enabled = !linksEmpty && hasFolders,
            onClick = { publish(ModpackMsg.FilesCollapseAllFolders(folderIds)) },
        )
        if (!showSelection) {
            Text(
                text = "Fetch writes Pakku's files to disk. Sync reads disk changes into Pakku.",
                color = JewelTheme.contentColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Tri-state "select all" box aligned with the list's checkbox column. */
@Composable
private fun MasterSelectionCheckbox(
    keys: Collection<String>,
    selectedKeys: Set<String>,
    publish: (ModpackMsg) -> Unit,
    lastClickedIndex: MutableState<Int?>?,
) {
    val selectedCount = keys.count { it in selectedKeys }
    val state = when {
        keys.isEmpty() || selectedCount == 0 -> ToggleableState.Off
        selectedCount == keys.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Box(
        modifier = Modifier.width(ProjectsListCheckboxColumnWidth),
        contentAlignment = Alignment.Center,
    ) {
        TriStateCheckbox(
            state = state,
            enabled = keys.isNotEmpty(),
            onClick = {
                if (state == ToggleableState.On) {
                    publish(ModpackMsg.ProjectsCleared())
                } else {
                    publish(ModpackMsg.ProjectsSelected(keys.toSet()))
                }
                lastClickedIndex?.value = null
            },
            modifier = Modifier.padding(4.dp),
        )
    }
}

/** Master checkbox; Name / Last updated sort on the right. */
@Composable
fun ListControls(
    publish: (ModpackMsg) -> Unit,
    model: ProjectsTabModel,
    lastClickedIndex: MutableState<Int?>? = null,
    sourceProjects: List<Project>? = null,
    ignoreUpdatesOnly: Boolean = false,
    selectableKeys: Collection<String>? = null,
    showSelection: Boolean = true,
) {
    // Live lock-file list; update mutates it off the UI thread.
    val projects = (sourceProjects ?: model.lockFile?.get()?.getAllProjects()).snapshotList()
    val filteredProjects = projects.filter { it.matchesProjectsListFilters(model, ignoreUpdatesOnly) }
    val sortOrder = model.sortOrder
    val keys = selectableKeys?.toList() ?: filteredProjects.map { it.uiKey() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSelection) {
            MasterSelectionCheckbox(
                keys = keys,
                selectedKeys = model.selectedProjectKeys,
                publish = publish,
                lastClickedIndex = lastClickedIndex,
            )
        }

        Spacer(Modifier.weight(1f))

        SortLabel(
            label = "Name",
            active = sortOrder is SortOrder.Name,
            ascending = (sortOrder as? SortOrder.Name)?.ascending == true,
            onClick = {
                publish(
                    ModpackMsg.SortOrderChanged(
                        when {
                            sortOrder is SortOrder.Name && sortOrder.ascending ->
                                SortOrder.Name(ascending = false)
                            else -> SortOrder.Name(ascending = true)
                        },
                    ),
                )
            },
        )

        SortLabel(
            label = "Last updated",
            active = sortOrder is SortOrder.LastUpdated,
            ascending = (sortOrder as? SortOrder.LastUpdated)?.ascending == true,
            onClick = {
                publish(
                    ModpackMsg.SortOrderChanged(
                        when {
                            sortOrder is SortOrder.LastUpdated && !sortOrder.ascending ->
                                SortOrder.LastUpdated(ascending = true)
                            else -> SortOrder.LastUpdated(ascending = false)
                        },
                    ),
                )
            },
        )
    }
}

@Composable
private fun SortLabel(
    label: String,
    active: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .clickableHover(scaleOnHover = true, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = if (active) JewelTheme.contentColor else JewelTheme.contentColor.copy(alpha = 0.65f),
            fontSize = 12.sp,
        )
        if (active) {
            val icon = if (ascending) AllIconsKeys.Gutter.Fold else AllIconsKeys.Gutter.FoldBottom
            Icon(
                icon,
                contentDescription = "Sort direction",
                modifier = Modifier.size(14.dp).padding(start = 4.dp),
            )
        }
    }
}
