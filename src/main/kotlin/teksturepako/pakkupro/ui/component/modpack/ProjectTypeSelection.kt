package teksturepako.pakkupro.ui.component.modpack

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.component.ContentBox
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectTypeSelection()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val label = "Type:"

    if (modpackUiState.editingProject != null) {
        var projectSideState: ProjectType? by remember { mutableStateOf(modpackUiState.editingProject?.type) }

        val buttons = ProjectType.entries.map { entry ->
            SegmentedControlButtonData(
                selected = projectSideState == entry,
                content = { _ ->
                    Text(entry.name)
                },
                onSelect = {
                    projectSideState = entry

                    coroutineScope.launch {
                        ModpackViewModel.writeEditingProjectToDisk {
                            this.type = entry
                        }
                    }
                }
            )
        }

        // Use Column to center align vertically
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            // Use Row to center align horizontally
            Row(
                verticalAlignment = Alignment.CenterVertically, // Center align vertically
            ) {
                Column {
                    Row {
                        ContentBox(Modifier.padding(2.dp)) {
                            Text(label)
                        }
                    }
                    Row {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    ModpackViewModel.writeEditingProjectToDisk {
                                        this.type = null
                                    }
                                    ModpackViewModel.loadFromDisk()
                                }
                                projectSideState = modpackUiState.selectedProject?.type
                            },
                            modifier = Modifier.padding(horizontal = 4.dp).size(30.dp)
                        ) {
                            Icon(
                                PakkuDesktopIcons.remove,
                                "reset",
                                tint = Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Wrap the buttons in a FlowRow to ensure wrapping on small displays
                FlowRow(
                    modifier = Modifier.padding(2.dp)
                ) {
                    buttons.forEach { button ->
                        SegmentedControl(
                            listOf(button),
                            Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
    else if (modpackUiState.selectedProject?.type != null)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center // Center align vertically
        ) {
            ContentBox(
                Modifier.padding(2.dp)
            ) {
                Text(label)
            }

            ContentBox(
                Modifier.padding(2.dp)
            ) {
                Text(modpackUiState.selectedProject!!.type.name)
            }
        }
    }
}
