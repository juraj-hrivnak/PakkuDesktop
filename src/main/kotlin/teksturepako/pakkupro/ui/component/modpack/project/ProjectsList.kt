package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.component.ProjectCard
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import java.net.URI

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ProjectsList(coroutineScope: CoroutineScope = rememberCoroutineScope()) {
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val lockFile = modpackUiState.lockFile?.get() ?: return
    val scrollState = rememberLazyListState()

    // For shift+click functionality
    var lastClickedIndex by remember { mutableStateOf<Int?>(null) }
    var shiftPressed by remember { mutableStateOf(false) }

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

    LazyColumn(
        Modifier
            .padding(start = 26.dp, end = 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout((placeable.width - offsetPx * 2).coerceAtLeast(40), placeable.height) {
                    placeable.placeRelative(-offsetPx, 0)
                }
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        scrollState.scrollBy(-delta)
                    }
                }
            )
            .background(JewelTheme.globalColors.panelBackground)
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
                    KeyEventType.KeyUp -> if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight)
                    {
                        shiftPressed = false
                    }
                }
                true
            },
        scrollState
    ) {
        val filteredProjects = lockFile.getAllProjects().filter(modpackUiState.projectsFilter)

        filteredProjects.mapIndexed { index, project ->
            item {
                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        IconButton(
                            onClick = { ModpackViewModel.selectProject(project) },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(30.dp),
                            enabled = modpackUiState.selectedProject != project
                        ) {
                            Tooltip({ Text("Properties") }) {
                                Icon(
                                    key = PakkuDesktopIcons.properties,
                                    contentDescription = "properties",
                                    tint = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Checkbox(
                            checked = ModpackViewModel.SelectedProjects.isSelected(project),
                            onCheckedChange = { checked ->
                                if (shiftPressed && lastClickedIndex != null)
                                {
                                    val startIdx = minOf(lastClickedIndex!!, index)
                                    val endIdx = maxOf(lastClickedIndex!!, index)
                                    val projectsInRange = filteredProjects.slice(startIdx..endIdx)

                                    if (checked)
                                    {
                                        ModpackViewModel.SelectedProjects.select(projectsInRange)
                                    }
                                    else
                                    {
                                        ModpackViewModel.SelectedProjects.deselect { p ->
                                            projectsInRange.any { it.pakkuId == p.pakkuId }
                                        }
                                    }
                                }
                                else
                                {
                                    ModpackViewModel.SelectedProjects.toggle(project)
                                    lastClickedIndex = index
                                }
                            },
                            enabled = true,
                            modifier = Modifier.padding(4.dp)
                        )

                        ProjectCard(project)
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