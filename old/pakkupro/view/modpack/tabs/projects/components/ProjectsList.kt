package pakkupro.view.modpack.tabs.projects.components

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
import androidx.compose.runtime.MutableState
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
import org.jetbrains.jewel.ui.icon.PathIconKey
import pakkupro.viewmodel.Icons
import pakkupro.viewmodel.PakkuDesktopIcons
import teksturepako.pakku.api.projects.Project

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectsList(
    projects: MutableState<List<Project>>,
    searchFilter: MutableState<(Project) -> Boolean>,
    selectedProject: MutableState<Project?>,
    coroutineScope: CoroutineScope,
)
{
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

        for (project in projects.value.filter(searchFilter.value))
        {
            item {
                Row(Modifier.padding(vertical = 4.dp)) {
//                    Column {
//                        var iconUrl by remember { mutableStateOf<String?>(null) }
//
//                        LaunchedEffect(Unit) {
//                            val prj = updatedProjects.await()
//                                .filter(searchFilter.value)
//                                .find { it isAlmostTheSameAs project }
//
//                            iconUrl = prj?.icon?.values?.firstOrNull()
//                        }
//
//                        iconUrl?.let { url ->
//                            IconButton(
//                                onClick = { selectedProject.value = project },
//                                Modifier
//                                    .padding(4.dp)
//                                    .size(50.dp)
//                            ) {
//                                IconFromUrl(
//                                    url, Modifier
//                                        .padding(4.dp)
//                                        .size(50.dp)
//                                        .clip(shape = RoundedCornerShape(5.dp))
//                                )
//                            }
//                        }
//                    }

                    Column {
                        IconButton(
                            onClick = { selectedProject.value = project },
                            Modifier.padding(horizontal = 4.dp).size(30.dp)
                        ) {
                            Tooltip({ Text("Properties") }) {
                                Icon(
                                    key = PathIconKey(
                                        "icons/properties.svg", Icons::class.java
                                    ),
                                    contentDescription = "settings",
                                    tint = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    hints = arrayOf()
                                )
                            }
                        }

//                        var canUpdate by remember { mutableStateOf(false) }
//                        var showPopup by remember { mutableStateOf(false) }
//
//                        LaunchedEffect(Unit) {
//                            if (updatedProjects.await() containProject project)
//                            {
//                                canUpdate = true
//                            }
//                        }
//
//                        if (!canUpdate)
//                        {
//                            IconButton(
//                                onClick = { },
//                                Modifier.padding(horizontal = 4.dp).size(30.dp),
//                                enabled = false
//                            ) {
//                                Icon(
//                                    key = PathIconKey(
//                                        "icons/download.svg", Icons::class.java
//                                    ),
//                                    contentDescription = "settings",
//                                    tint = Color.Gray,
//                                    modifier = Modifier.padding(horizontal = 4.dp),
//                                    hints = arrayOf()
//                                )
//                            }
//                        }
//                        else
//                        {
//                            IconButton(
//                                onClick = {
//                                    showPopup = true
//                                },
//                                Modifier.padding(horizontal = 4.dp).size(30.dp)
//                            ) {
//                                Tooltip({ Text("Update") }) {
//                                    Icon(
//                                        key = PathIconKey(
//                                            "icons/download.svg", Icons::class.java
//                                        ),
//                                        contentDescription = "settings",
//                                        tint = if (theme.isDark()) Color.Cyan else Color.Blue,
//                                        modifier = Modifier.padding(horizontal = 4.dp),
//                                        hints = arrayOf()
//                                    )
//                                }
//                            }
//                        }
//
//                        if (showPopup)
//                        {
//                            Box(Modifier.background(Color.Transparent)) {
//                                PError("Project updated")
//                            }
//
//                            LaunchedEffect(Unit) {
//                                delay(600)
//                                showPopup = false
//                            }
//                        }
                    }

                    Column(verticalArrangement = Arrangement.SpaceEvenly) {
                        // Name
                        project.name.values.firstOrNull()?.let {
                            Text(it, Modifier.padding(4.dp))
                        }

                        Row {
                            for (provider in project.getProviders())
                            {
                                val provIcon = when (provider.serialName)
                                {
                                    "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                                    "github"     -> PakkuDesktopIcons.Platforms.gitHub
                                    "modrinth"   -> PakkuDesktopIcons.Platforms.modrinth
                                    else         -> null
                                }

                                provIcon?.let {
                                    Icon(it, provider.name, Modifier.padding(4.dp).size(25.dp))
                                }
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
