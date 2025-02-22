package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakkupro.ui.component.ProjectCard
import teksturepako.pakkupro.ui.modifier.clickableHover
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import java.net.URI

sealed class SortOrder(open val ascending: Boolean)
{
    data class Name(override val ascending: Boolean) : SortOrder(ascending)
    data class LastUpdated(override val ascending: Boolean) : SortOrder(ascending)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ProjectsList(coroutineScope: CoroutineScope = rememberCoroutineScope()) {
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val lockFile = modpackUiState.lockFile?.get() ?: return
    val scrollState = rememberLazyListState()

    // For shift+click functionality
    var lastClickedIndex by remember { mutableStateOf<Int?>(null) }
    var shiftPressed by remember { mutableStateOf(false) }

    // For sorting
    var sortOrder: SortOrder by remember { mutableStateOf(SortOrder.Name(ascending = true)) }

    /** How much space you want to remove from the start and the end of the Icon */
    val offsetDp = 10.dp
    val density = LocalDensity.current
    /** Offset in pixels */
    val offsetPx = remember(offsetDp) { density.run { offsetDp.roundToPx() } }

    var showTargetBorder by remember { mutableStateOf(false) }
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                showTargetBorder = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                showTargetBorder = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                println("Action at the target: ${event.action}")

                if (event.dragData() !is DragData.FilesList) return false

                val pathUri = (event.dragData() as DragData.FilesList)
                    .readFiles()
                    .first()
                    .let(::URI)

                println(pathUri.path)

                coroutineScope.launch {
                    readPathBytesOrNull(pathUri.path)
                }

                return true
            }
        }
    }

    Column(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout((placeable.width - offsetPx * 2).coerceAtLeast(40), placeable.height) {
                    placeable.placeRelative(-offsetPx, 0)
                }
            }
    ) {
        // Top control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .padding(end = 4.dp)
            ) {
                Checkbox(
                    checked = modpackUiState.lockFile?.get()?.getAllProjects()
                        ?.filter(modpackUiState.projectsFilter)
                        ?.all { ModpackViewModel.SelectedProjects.isSelected(it) } == true,
                    onCheckedChange = { checked ->
                        val filteredProjects = modpackUiState.lockFile?.get()?.getAllProjects()
                            ?.filter(modpackUiState.projectsFilter) ?: return@Checkbox

                        if (checked) {
                            ModpackViewModel.SelectedProjects.select(filteredProjects)
                        } else {
                            ModpackViewModel.SelectedProjects.clear()
                        }
                        lastClickedIndex = null
                    },
                    modifier = Modifier.padding(4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickableHover(
                    scaleOnHover = true
                ) {
                    sortOrder = when {
                        sortOrder is SortOrder.Name && sortOrder.ascending -> SortOrder.Name(ascending = false)
                        else -> SortOrder.Name(ascending = true)
                    }
                }
            ) {
                Text(
                    text = "Name",
                    color = JewelTheme.contentColor
                )

                (if (sortOrder is SortOrder.Name && sortOrder.ascending)
                    AllIconsKeys.Gutter.Fold
                else if (sortOrder is SortOrder.Name)
                    AllIconsKeys.Gutter.FoldBottom
                else null)?.let {
                    Icon(
                        it,
                        contentDescription = "Sort direction",
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickableHover(
                    scaleOnHover = true
                ) {
                    sortOrder = when {
                        sortOrder is SortOrder.LastUpdated && !sortOrder.ascending -> SortOrder.LastUpdated(ascending = true)
                        else -> SortOrder.LastUpdated(ascending = false)
                    }
                }
            ) {
                Text(
                    text = "Last Updated",
                    color = JewelTheme.contentColor
                )

                (if (sortOrder is SortOrder.LastUpdated && sortOrder.ascending)
                    AllIconsKeys.Gutter.Fold
                else if (sortOrder is SortOrder.LastUpdated)
                    AllIconsKeys.Gutter.FoldBottom
                else null)?.let {
                    Icon(
                        it,
                        contentDescription = "Sort direction",
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            Modifier
                .padding(start = 26.dp, end = 16.dp)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout((placeable.width - offsetPx * 2).coerceAtLeast(40), placeable.height) {
                        placeable.placeRelative(-offsetPx, 0)
                    }
                }
                .then(
                    if (showTargetBorder)
                        Modifier.border(
                            width = 3.dp,
                            color = JewelTheme.globalColors.outlines.focused,
                            shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                        )
                    else Modifier
                )
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dragAndDropTarget
                )
                .onKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> shiftPressed = event.isShiftPressed
                        KeyEventType.KeyUp -> if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                            shiftPressed = false
                        }
                    }
                    true
                },
            scrollState
        ) {
            val filteredProjects = lockFile.getAllProjects()
                .filter(modpackUiState.projectsFilter)
                .let { projects ->
                    when (sortOrder) {
                        is SortOrder.Name ->
                        {
                            if (sortOrder.ascending)
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
                            if (sortOrder.ascending)
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
                item {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .padding(top = 7.dp)
                                .padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = ModpackViewModel.SelectedProjects.isSelected(project),
                                    onCheckedChange = { checked ->
                                        if (shiftPressed && lastClickedIndex != null) {
                                            val startIdx = minOf(lastClickedIndex!!, index)
                                            val endIdx = maxOf(lastClickedIndex!!, index)
                                            val projectsInRange = filteredProjects.slice(startIdx..endIdx)

                                            if (checked) {
                                                ModpackViewModel.SelectedProjects.select(projectsInRange)
                                            } else {
                                                ModpackViewModel.SelectedProjects.deselect { p ->
                                                    projectsInRange.any { it.pakkuId == p.pakkuId }
                                                }
                                            }
                                        } else {
                                            ModpackViewModel.SelectedProjects.toggle(project)
                                            lastClickedIndex = index
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
    }

    VerticalScrollbar(
        modifier = Modifier.fillMaxHeight(),
        scrollState = scrollState
    )
}