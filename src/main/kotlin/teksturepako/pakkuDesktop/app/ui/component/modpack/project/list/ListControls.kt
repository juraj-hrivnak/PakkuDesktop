/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

/** Must match the checkbox column in [ListImpl] for pixel alignment. */
internal val ProjectsListCheckboxColumnWidth = 40.dp

/** Master checkbox + select-all / clear + sort. Filters live in ProjectFilter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListControls(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    lastClickedIndex: MutableState<Int?>,
) {
    val projects = model.lockFile?.get()?.getAllProjects() ?: emptyList()
    val filteredProjects = projects.filter { it.matchesProjectsListFilters(model) }
    val sortOrder = model.sortOrder
    val selectableIds = filteredProjects.mapNotNull { it.pakkuId }
    val allSelected = selectableIds.isNotEmpty() && selectableIds.all { it in model.selectedPakkuIds }
    val selectedCount = model.selectedPakkuIds.size

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Same slot as row checkboxes in ListImpl (40.dp + end pad + centered Checkbox).
            Box(
                modifier = Modifier
                    .width(ProjectsListCheckboxColumnWidth)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            publish(ModpackMsg.ProjectsSelected(selectableIds.toSet()))
                        } else {
                            publish(ModpackMsg.ProjectsCleared())
                        }
                        lastClickedIndex.value = null
                    },
                    modifier = Modifier.padding(4.dp),
                )
            }
            IconButton(
                enabled = selectableIds.isNotEmpty() && !allSelected,
                onClick = {
                    publish(ModpackMsg.SelectAllFilteredRequested)
                    lastClickedIndex.value = null
                },
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Selectall,
                    contentDescription = "Select all",
                    tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                    hints = arrayOf(),
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                enabled = selectedCount > 0,
                onClick = {
                    publish(ModpackMsg.ProjectsCleared())
                    lastClickedIndex.value = null
                },
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Unselectall,
                    contentDescription = "Clear selection",
                    tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                    hints = arrayOf(),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.size(4.dp))

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
