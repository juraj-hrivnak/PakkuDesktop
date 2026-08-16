/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.LocalShiftPressed
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectCard
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover

@Composable
fun ListImpl(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    lastClickedIndex: MutableState<Int?>,
)
{
    val lockFile = model.lockFile?.get() ?: return
    val shiftPressed = LocalShiftPressed.current

    val scrollState = remember { LazyListState(0, 0) }
    val filteredProjects = model.filteredAndSortedProjects(lockFile.getAllProjects())

    Row(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
                .allowDragAndDrop { paths ->
                    publish(ModpackMsg.FilesDropped(paths.map { it.toString() }))
                },
            state = scrollState,
        ) {
            itemsIndexed(
                items = filteredProjects,
                key = { index, project -> "${project.pakkuId ?: "noid"}#$index" },
            ) { index, project ->
                val checked = project.pakkuId in model.selectedPakkuIds
                val focused = project.pakkuId != null &&
                    project.pakkuId == model.selectedProject?.pakkuId

                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .padding(top = 7.dp, end = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { nextChecked ->
                                if (shiftPressed && lastClickedIndex.value != null) {
                                    val start = minOf(lastClickedIndex.value!!, index)
                                    val end = maxOf(lastClickedIndex.value!!, index)
                                    val ids = filteredProjects.slice(start..end)
                                        .mapNotNull(Project::pakkuId)
                                        .toSet()
                                    if (nextChecked) publish(ModpackMsg.ProjectsSelected(ids))
                                    else publish(ModpackMsg.ProjectsDeselected(ids))
                                } else {
                                    val id = project.pakkuId ?: return@Checkbox
                                    if (nextChecked) publish(ModpackMsg.ProjectsSelected(setOf(id)))
                                    else publish(ModpackMsg.ProjectsDeselected(setOf(id)))
                                    lastClickedIndex.value = index
                                }
                            },
                            modifier = Modifier.padding(4.dp),
                        )
                    }

                    ProjectCard(
                        project = project,
                        selected = checked || focused,
                        updateInfo = project.pakkuId?.let { model.updatePreviews?.get(it) },
                        modifier = Modifier
                            .weight(1f)
                            .clickableHover(
                                pressed = if (focused) true else null,
                                enabled = !focused,
                            ) {
                                publish(ModpackMsg.ProjectSelected(project))
                                lastClickedIndex.value = index
                            },
                    )
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            scrollState = scrollState,
        )
    }
}
