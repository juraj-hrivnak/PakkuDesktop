package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
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
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectCard
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.SortOrder

@Composable
fun ListImpl(lastClickedIndex: MutableState<Int?>, shiftPressed: MutableState<Boolean>)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val lockFile = modpackUiState.lockFile?.get() ?: return

    val offsetDp = 10.dp
    val density = LocalDensity.current

    val offsetPx = remember(offsetDp) { density.run { offsetDp.roundToPx() } }

    Row(
        Modifier.padding(vertical = 8.dp)
    ) {
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
            },
            ModpackViewModel.projectsScrollState.value
        ) {
            val filteredProjects = lockFile.getAllProjects().filter(modpackUiState.projectsFilter).let { projects ->
                when (modpackUiState.sortOrder)
                {
                    is SortOrder.Name        ->
                    {
                        if (modpackUiState.sortOrder.ascending)
                        {
                            projects.sortedBy { it.name.values.firstOrNull() }
                        }
                        else
                        {
                            projects.sortedByDescending { it.name.values.firstOrNull() }
                        }
                    }

                    is SortOrder.LastUpdated ->
                    {
                        if (modpackUiState.sortOrder.ascending)
                        {
                            projects.sortedBy { it.getLatestFile(Provider.providers)?.datePublished }
                        }
                        else
                        {
                            projects.sortedByDescending { it.getLatestFile(Provider.providers)?.datePublished }
                        }
                    }
                }
            }

            filteredProjects.mapIndexed { index, project ->
                item(
                    key = project.pakkuId
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.width(40.dp).padding(top = 7.dp).padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = ModpackViewModel.ProjectsSelection.isSelected(project),
                                    onCheckedChange = { checked ->
                                        if (shiftPressed.value && lastClickedIndex.value != null)
                                        {
                                            val startIdx = minOf(lastClickedIndex.value!!, index)
                                            val endIdx = maxOf(lastClickedIndex.value!!, index)

                                            val projectsInRange = filteredProjects.slice(startIdx..endIdx)

                                            if (checked)
                                            {
                                                ModpackViewModel.ProjectsSelection.select(projectsInRange)
                                            }
                                            else
                                            {
                                                ModpackViewModel.ProjectsSelection.deselect { p ->
                                                    projectsInRange.any { it.pakkuId == p.pakkuId }
                                                }
                                            }
                                        }
                                        else
                                        {
                                            ModpackViewModel.ProjectsSelection.toggle(project)
                                            lastClickedIndex.value = index
                                        }
                                    },
                                    enabled = true,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        ProjectCard(
                            project = project,
                            modifier = Modifier
                                .weight(1f)
                                .clickableHover(
                                    pressed = if (modpackUiState.selectedProject == project) true else null,
                                    enabled = modpackUiState.selectedProject != project
                                ) {
                                    ModpackViewModel.selectProject(project)
                                }
                        )
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.fillMaxHeight(), scrollState = ModpackViewModel.projectsScrollState.value
        )
    }
}
