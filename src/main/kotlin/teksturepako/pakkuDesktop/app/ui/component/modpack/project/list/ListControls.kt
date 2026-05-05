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
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

@Composable
fun ListControls(
    publish: (AppMsg) -> Unit,
    model: AppModel,
    lastClickedIndex: MutableState<Int?>,
) {
    val modpack = model.modpack
    val projects = modpack.lockFile?.get()?.getAllProjects() ?: emptyList()
    val filteredProjects = projects.filter { p ->
        modpack.projectsFilterText.isEmpty() ||
            p.name.values.any { modpack.projectsFilterText.lowercase() in it.lowercase() } ||
            modpack.projectsFilterText in p
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(40.dp).padding(end = 4.dp)) {
            Checkbox(
                checked = filteredProjects.isNotEmpty() &&
                    filteredProjects.all { it.pakkuId in modpack.selectedPakkuIds },
                onCheckedChange = { checked ->
                    if (checked) {
                        publish(AppMsg.Modpack.ProjectsSelected(filteredProjects.mapNotNull { it.pakkuId }.toSet()))
                    } else {
                        publish(AppMsg.Modpack.ProjectsCleared())
                    }
                    lastClickedIndex.value = null
                },
                modifier = Modifier.padding(4.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                publish(AppMsg.Modpack.SortOrderChanged(
                    when {
                        modpack.sortOrder is SortOrder.Name && modpack.sortOrder.ascending -> SortOrder.Name(ascending = false)
                        else -> SortOrder.Name(ascending = true)
                    }
                ))
            }
        ) {
            Text(text = "Name", color = JewelTheme.contentColor)
            when {
                modpack.sortOrder is SortOrder.Name && modpack.sortOrder.ascending -> AllIconsKeys.Gutter.Fold
                modpack.sortOrder is SortOrder.Name                               -> AllIconsKeys.Gutter.FoldBottom
                else -> null
            }?.let {
                Icon(it, contentDescription = "Sort direction", modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                publish(AppMsg.Modpack.SortOrderChanged(
                    when {
                        modpack.sortOrder is SortOrder.LastUpdated && !modpack.sortOrder.ascending -> SortOrder.LastUpdated(ascending = true)
                        else -> SortOrder.LastUpdated(ascending = false)
                    }
                ))
            }
        ) {
            Text(text = "Last Updated", color = JewelTheme.contentColor)
            when {
                modpack.sortOrder is SortOrder.LastUpdated && modpack.sortOrder.ascending -> AllIconsKeys.Gutter.Fold
                modpack.sortOrder is SortOrder.LastUpdated                               -> AllIconsKeys.Gutter.FoldBottom
                else -> null
            }?.let {
                Icon(it, contentDescription = "Sort direction", modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
        }
    }
}
