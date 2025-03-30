package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.background
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
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakkupro.ui.component.dialog.DismissibleDialog
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.modifier.allowDragAndDrop
import teksturepako.pakkupro.ui.modifier.clickableHover
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SortOrder

@Composable
fun ProjectsList()
{
    // For shift+click functionality
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }
    val shiftPressed = remember { mutableStateOf(false) }

    Column {
        Spacer(Modifier.fillMaxWidth().padding(vertical = 4.dp))

        // Filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectFilter()
        }

        // Controls
        Column {
            ListControls(lastClickedIndex)
        }

        Spacer(
            Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth()
        )

        // Main content with scrollbar
        Box(modifier = Modifier.weight(1f)) {
            List(lastClickedIndex, shiftPressed)
        }

        // Bottom border
        Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())

        // Actions at bottom
        ListActions()
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListActions()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val selectedProjects = modpackUiState.selectedProjectsMap.values.size
    val projects = modpackUiState.lockFile?.get()?.getAllProjects()?.filter { project ->
        modpackUiState.selectedProjectsMap[project.pakkuId]?.let { it(project) } == true
    } ?: emptyList()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val message = when
        {
            modpackUiState.lockFile?.get()?.getAllProjects()?.size == selectedProjects ->
            {
                "All $selectedProjects projects selected"
            }
            selectedProjects > 1  -> "$selectedProjects projects selected"
            selectedProjects == 1 -> "1 project selected"
            else                  -> ""
        }

        Text(message)
    }

    Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultButton(
                onClick = {

                },
                enabled = selectedProjects > 0
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Update")
                    Icon(
                        key = AllIconsKeys.Actions.CheckOut,
                        contentDescription = "update",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            var removePopupVisible by remember { mutableStateOf(false) }

            DismissibleDialog(
                removePopupVisible,
                onDismiss = {
                    removePopupVisible = false
                }
            ) {
                FlowColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Header("Do you want to remove this project?")
                    Spacer(Modifier.height(8.dp))
                    projects.map {
                        ProjectCard(it)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    removePopupVisible = true
                },
                enabled = selectedProjects > 0
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Remove")
                    Icon(
                        key = AllIconsKeys.General.Delete,
                        contentDescription = "remove",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListControls(lastClickedIndex: MutableState<Int?>)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(40.dp).padding(end = 4.dp)
        ) {
            Checkbox(
                checked = modpackUiState.lockFile?.get()?.getAllProjects()
                    ?.filter(modpackUiState.projectsFilter)
                    ?.all { ModpackViewModel.ProjectsSelection.isSelected(it) } == true,
                onCheckedChange = { checked ->
                    val filteredProjects = modpackUiState.lockFile?.get()
                        ?.getAllProjects()
                        ?.filter(modpackUiState.projectsFilter)
                        ?: return@Checkbox

                    if (checked)
                    {
                        ModpackViewModel.ProjectsSelection.select(filteredProjects)
                    }
                    else
                    {
                        ModpackViewModel.ProjectsSelection.clear()
                    }

                    lastClickedIndex.value = null
                },
                modifier = Modifier.padding(4.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                ModpackViewModel.updateSortOrder(
                    when
                    {
                        modpackUiState.sortOrder is SortOrder.Name && modpackUiState.sortOrder.ascending ->
                        {
                            SortOrder.Name(ascending = false)
                        }

                        else                                                                             ->
                        {
                            SortOrder.Name(ascending = true)
                        }
                    }
                )
            }) {
            Text(
                text = "Name", color = JewelTheme.contentColor
            )

            when
            {
                modpackUiState.sortOrder is SortOrder.Name && modpackUiState.sortOrder.ascending ->
                {
                    AllIconsKeys.Gutter.Fold
                }

                modpackUiState.sortOrder is SortOrder.Name                                       ->
                {
                    AllIconsKeys.Gutter.FoldBottom
                }

                else                                                                             -> null
            }?.let {
                Icon(
                    it,
                    contentDescription = "Sort direction",
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                ModpackViewModel.updateSortOrder(
                    when
                    {
                        modpackUiState.sortOrder is SortOrder.LastUpdated && !modpackUiState.sortOrder.ascending ->
                        {
                            SortOrder.LastUpdated(ascending = true)
                        }

                        else                                                                                     ->
                        {
                            SortOrder.LastUpdated(ascending = false)
                        }
                    }
                )
            }) {
            Text(
                text = "Last Updated", color = JewelTheme.contentColor
            )

            when
            {
                modpackUiState.sortOrder is SortOrder.LastUpdated && modpackUiState.sortOrder.ascending ->
                {
                    AllIconsKeys.Gutter.Fold
                }

                modpackUiState.sortOrder is SortOrder.LastUpdated                                       ->
                {
                    AllIconsKeys.Gutter.FoldBottom
                }

                else                                                                                    -> null
            }?.let {
                Icon(
                    it,
                    contentDescription = "Sort direction",
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun List(lastClickedIndex: MutableState<Int?>, shiftPressed: MutableState<Boolean>)
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
                    is SortOrder.Name ->
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
