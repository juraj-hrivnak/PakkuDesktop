/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

@Composable
fun ListControls(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    lastClickedIndex: MutableState<Int?>,
) {
    val projects = model.lockFile?.get()?.getAllProjects() ?: emptyList()
    val filteredProjects = projects.filter { p ->
        model.projectsFilterText.isEmpty() ||
            p.name.values.any { model.projectsFilterText.lowercase() in it.lowercase() } ||
            model.projectsFilterText in p
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(40.dp).padding(end = 4.dp)) {
            Checkbox(
                checked = filteredProjects.isNotEmpty() &&
                    filteredProjects.all { it.pakkuId in model.selectedPakkuIds },
                onCheckedChange = { checked ->
                    if (checked) {
                        publish(ModpackMsg.ProjectsSelected(filteredProjects.mapNotNull { it.pakkuId }.toSet()))
                    } else {
                        publish(ModpackMsg.ProjectsCleared())
                    }
                    lastClickedIndex.value = null
                },
                modifier = Modifier.padding(4.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                publish(ModpackMsg.SortOrderChanged(
                    when {
                        model.sortOrder is SortOrder.Name && model.sortOrder.ascending -> SortOrder.Name(ascending = false)
                        else -> SortOrder.Name(ascending = true)
                    }
                ))
            }
        ) {
            Text(text = "Name", color = JewelTheme.contentColor)
            when {
                model.sortOrder is SortOrder.Name && model.sortOrder.ascending -> AllIconsKeys.Gutter.Fold
                model.sortOrder is SortOrder.Name                             -> AllIconsKeys.Gutter.FoldBottom
                else -> null
            }?.let {
                Icon(it, contentDescription = "Sort direction", modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                publish(ModpackMsg.SortOrderChanged(
                    when {
                        model.sortOrder is SortOrder.LastUpdated && !model.sortOrder.ascending -> SortOrder.LastUpdated(ascending = true)
                        else -> SortOrder.LastUpdated(ascending = false)
                    }
                ))
            }
        ) {
            Text(text = "Last Updated", color = JewelTheme.contentColor)
            when {
                model.sortOrder is SortOrder.LastUpdated && model.sortOrder.ascending -> AllIconsKeys.Gutter.Fold
                model.sortOrder is SortOrder.LastUpdated                             -> AllIconsKeys.Gutter.FoldBottom
                else -> null
            }?.let {
                Icon(it, contentDescription = "Sort direction", modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
        }
    }
}
