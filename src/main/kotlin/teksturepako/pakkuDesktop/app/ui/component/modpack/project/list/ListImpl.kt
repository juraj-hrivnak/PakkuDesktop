/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectCard
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

@Composable
fun ListImpl(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    lastClickedIndex: MutableState<Int?>,
    shiftPressed: MutableState<Boolean>,
)
{
    val lockFile = model.lockFile?.get() ?: return

    val scrollState = remember { LazyListState(0, 0) }
    val offsetDp = 10.dp
    val density = LocalDensity.current
    val offsetPx = remember(offsetDp) { density.run { offsetDp.roundToPx() } }

    val filteredProjects = lockFile.getAllProjects().filter { p ->
        model.projectsFilterText.isEmpty() || p.name.values.any { model.projectsFilterText.lowercase() in it.lowercase() } || model.projectsFilterText in p
    }.let { projects ->
        when (model.sortOrder)
        {
            is SortOrder.Name -> if (model.sortOrder.ascending) projects.sortedBy { it.name.values.firstOrNull() }
            else projects.sortedByDescending { it.name.values.firstOrNull() }

            is SortOrder.LastUpdated -> if (model.sortOrder.ascending) projects.sortedBy { it.getLatestFile(Provider.providers)?.datePublished }
            else projects.sortedByDescending { it.getLatestFile(Provider.providers)?.datePublished }
        }
    }

    Row(Modifier.padding(vertical = 8.dp)) {
        LazyColumn(
            Modifier.padding(start = 26.dp, end = 16.dp).layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout((placeable.width - offsetPx * 2).coerceAtLeast(40), placeable.height) {
                    placeable.placeRelative(-offsetPx, 0)
                }
            }.allowDragAndDrop().onKeyEvent { event ->
                when (event.type)
                {
                    KeyEventType.KeyDown -> shiftPressed.value = event.isShiftPressed
                    KeyEventType.KeyUp   -> if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight)
                    {
                        shiftPressed.value = false
                    }
                }
                true
            }, scrollState
        ) {
            filteredProjects.mapIndexed { index, project ->
                item(key = project.pakkuId) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.width(40.dp).padding(top = 7.dp).padding(end = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = project.pakkuId in model.selectedPakkuIds, onCheckedChange = { checked ->
                                        if (shiftPressed.value && lastClickedIndex.value != null)
                                        {
                                            val start = minOf(lastClickedIndex.value!!, index)
                                            val end = maxOf(lastClickedIndex.value!!, index)
                                            val ids =
                                                filteredProjects.slice(start..end).mapNotNull(Project::pakkuId).toSet()
                                            if (checked) publish(ModpackMsg.ProjectsSelected(ids))
                                            else publish(ModpackMsg.ProjectsDeselected(ids))
                                        }
                                        else
                                        {
                                            val id = project.pakkuId ?: return@Checkbox
                                            if (checked) publish(ModpackMsg.ProjectsSelected(setOf(id)))
                                            else publish(ModpackMsg.ProjectsDeselected(setOf(id)))
                                            lastClickedIndex.value = index
                                        }
                                    }, enabled = true, modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        ProjectCard(
                            project = project,
                            modifier = Modifier.weight(1f).clickableHover(
                                pressed = if (model.selectedProject == project) true else null,
                                enabled = model.selectedProject != project
                            ) {
                                publish(ModpackMsg.ProjectSelected(project))
                            })
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            scrollState = scrollState
        )
    }
}
