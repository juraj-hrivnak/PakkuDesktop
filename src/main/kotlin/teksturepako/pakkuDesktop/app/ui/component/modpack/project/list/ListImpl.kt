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
import teksturepako.pakkuDesktop.app.actions.uiKey
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
                key = { index, project -> "${project.uiKey()}#$index" },
            ) { index, project ->
                val projectKey = project.uiKey()
                val checked = projectKey in model.selectedProjectKeys
                val focused = projectKey == model.selectedProject?.uiKey()

                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .width(ProjectsListCheckboxColumnWidth)
                            .padding(top = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { nextChecked ->
                                if (shiftPressed && lastClickedIndex.value != null) {
                                    val start = minOf(lastClickedIndex.value!!, index)
                                    val end = maxOf(lastClickedIndex.value!!, index)
                                    val keys = filteredProjects.slice(start..end)
                                        .map { it.uiKey() }
                                        .toSet()
                                    if (nextChecked) publish(ModpackMsg.ProjectsSelected(keys))
                                    else publish(ModpackMsg.ProjectsDeselected(keys))
                                } else {
                                    if (nextChecked) publish(ModpackMsg.ProjectsSelected(setOf(projectKey)))
                                    else publish(ModpackMsg.ProjectsDeselected(setOf(projectKey)))
                                    lastClickedIndex.value = index
                                }
                            },
                            modifier = Modifier.padding(4.dp),
                        )
                    }

                    ProjectCard(
                        project = project,
                        focused = focused,
                        checked = checked,
                        updateInfo = model.updatePreviews?.get(projectKey),
                        modifier = Modifier
                            .weight(1f)
                            .clickableHover(
                                pressed = if (focused) true else null,
                                onDoubleClick = {
                                    if (checked) publish(ModpackMsg.ProjectsDeselected(setOf(projectKey)))
                                    else publish(ModpackMsg.ProjectsSelected(setOf(projectKey)))
                                    lastClickedIndex.value = index
                                },
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
