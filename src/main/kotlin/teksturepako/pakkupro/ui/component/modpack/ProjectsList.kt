package teksturepako.pakkupro.ui.component.modpack

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectsList(coroutineScope: CoroutineScope)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val lockFile = modpackUiState.lockFile ?: return

    val scrollState = rememberLazyListState()

    /** How much space you want to remove from the start and the end of the Icon */
    val offsetDp = 10.dp

    val density = LocalDensity.current

    /** Offset in pixels */
    val offsetPx = remember(offsetDp) { density.run { offsetDp.roundToPx() } }

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
            .background(JewelTheme.globalColors.panelBackground),
        scrollState
    ) {
        lockFile.getAllProjects().filter(modpackUiState.projectsFilter).map { project ->
            item {
                Row(Modifier.padding(vertical = 4.dp)) {
                    Column {
                        IconButton(
                            onClick = { ModpackViewModel.selectProject(project) },
                            Modifier.padding(horizontal = 4.dp).size(30.dp)
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
                    }
                    Column(verticalArrangement = Arrangement.SpaceEvenly) {
                        Row {
                            project.name.values.firstOrNull()?.let {
                                Text(it, Modifier.padding(4.dp))
                            }
                        }
                        Row {
                            project.getProviders().map { provider ->
                                val provIcon = when (provider.serialName)
                                {
                                    "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                                    "github"     -> PakkuDesktopIcons.Platforms.gitHub
                                    "modrinth"   -> PakkuDesktopIcons.Platforms.modrinth
                                    else         -> null
                                }

                                provIcon
                                    ?.let {
                                        Icon(it, provider.name, Modifier.padding(4.dp).size(25.dp))
                                    }
                                    ?: Text(provider.name)
                            }
                        }
                    }
                }
                Spacer(Modifier.background(JewelTheme.globalColors.borders.disabled).height(1.dp).fillMaxWidth())
            }
        }
    }

    VerticalScrollbar(
        modifier = Modifier.fillMaxHeight(),
        scrollState = scrollState,
    )
}
