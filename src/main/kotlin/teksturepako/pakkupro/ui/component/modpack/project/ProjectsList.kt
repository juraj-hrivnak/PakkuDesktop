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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import java.net.URI

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectsList(coroutineScope: CoroutineScope = rememberCoroutineScope())
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val lockFile = modpackUiState.lockFile?.getOrNull() ?: return

    val scrollState = rememberLazyListState()

    /** How much space you want to remove from the start and the end of the Icon */
    val offsetDp = 10.dp

    val density = LocalDensity.current

    /** Offset in pixels */
    val offsetPx = remember(offsetDp) { density.run { offsetDp.roundToPx() } }

    var showTargetBorder by remember { mutableStateOf(false) }
    val dragAndDropTarget = remember {
        object: DragAndDropTarget
        {
            // Highlights the border of a potential drop target
            override fun onStarted(event: DragAndDropEvent) {
                showTargetBorder = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                showTargetBorder = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Prints the type of action into system output every time
                // a drag-and-drop operation is concluded.
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
                    Modifier
                        .border(
                            width = 3.dp,
                            color = JewelTheme.globalColors.outlines.focused,
                            shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                        )
                else
                    Modifier
            )
            .dragAndDropTarget(
                // With "true" as the value of shouldStartDragAndDrop,
                // drag-and-drop operations are enabled unconditionally.
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            ),
        scrollState
    ) {
        lockFile.getAllProjects().filter(modpackUiState.projectsFilter).map { project ->
            item {
                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.width(300.dp),
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

                        Column(verticalArrangement = Arrangement.SpaceEvenly) {
                            project.name.values.firstOrNull()?.let {
                                Text(it, Modifier.padding(4.dp))
                            }

                            Row {
                                project.getProviders().map { provider ->
                                    val provIcon = when (provider.serialName) {
                                        "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                                        "github" -> PakkuDesktopIcons.Platforms.gitHub
                                        "modrinth" -> PakkuDesktopIcons.Platforms.modrinth
                                        else -> null
                                    }

                                    provIcon?.let {
                                        Icon(it, provider.name, Modifier.padding(4.dp).size(25.dp))
                                    } ?: Text(provider.name)
                                }
                            }
                        }
                    }

                    FlowRow {
                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (!project.redistributable)
                            {
                                Icon(
                                    PakkuDesktopIcons.exclamationTriangle, null,
                                    Modifier.size(25.dp), tint = Color.Red
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(project.type.name, Modifier.padding(4.dp), color = Color.Gray)
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            project.side?.name?.let { Text(it, Modifier.padding(4.dp), color = Color.Gray) }
                        }
                    }
                }

                Spacer(
                    Modifier
                        .background(JewelTheme.globalColors.borders.disabled)
                        .height(1.dp)
                        .fillMaxWidth()
                )
            }
        }
    }

    VerticalScrollbar(
        modifier = Modifier.fillMaxHeight(),
        scrollState = scrollState,
    )
}
