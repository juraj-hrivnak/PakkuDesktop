package teksturepako.pakkupro.ui.component.modpack.project.projectPropSelection

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.component.ContentBox
import teksturepako.pakkupro.ui.component.ImmediateTooltip
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import kotlin.enums.EnumEntries
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T : Enum<T>> NullableProjectEnumSelection(
    label: String,
    enumEntries: EnumEntries<T>,
    projectRef: KMutableProperty1<Project, T?>,
    projectConfigRef: KMutableProperty1<ConfigFile.ProjectConfig, T?>,
)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (modpackUiState.editingProject)
    {
        var buttonState by remember { mutableStateOf(modpackUiState.selectedProject?.let { projectRef(it) }) }

        val buttons = enumEntries.map { entry ->
            SegmentedControlButtonData(
                selected = buttonState == entry,
                content = { _ ->
                    Text(entry.name)
                },
                onSelect = {
                    buttonState = entry

                    coroutineScope.launch {
                        ModpackViewModel.writeEditingProjectToDisk {
                            projectConfigRef.set(this, entry)
                        }
                        ModpackViewModel.loadFromDisk()
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
                verticalAlignment = Alignment.Top // Center align vertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        ContentBox(Modifier.padding(2.dp)) {
                            Text(label)
                        }
                    }
                    Row {
                        ImmediateTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        ModpackViewModel.writeEditingProjectToDisk {
                                            projectConfigRef.set(this, null)
                                        }
                                        ModpackViewModel.loadFromDisk()
                                        buttonState = modpackUiState.selectedProject?.let { projectRef(it) }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp)
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback,
                                    "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
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
    else if (modpackUiState.selectedProject?.let { projectRef(it) } != null)
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
                Text(projectRef(modpackUiState.selectedProject!!)!!.name)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T : Enum<T>> ProjectEnumSelection(
    label: String,
    enumEntries: EnumEntries<T>,
    projectRef: KMutableProperty1<Project, T>,
    projectConfigRef: KMutableProperty1<ConfigFile.ProjectConfig, T?>,
)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (modpackUiState.editingProject)
    {
        var buttonState by remember { mutableStateOf(modpackUiState.selectedProject?.let { projectRef(it) }) }

        val buttons = enumEntries.map { entry ->
            SegmentedControlButtonData(
                selected = buttonState == entry,
                content = { _ ->
                    Text(entry.name)
                },
                onSelect = {
                    buttonState = entry

                    coroutineScope.launch {
                        ModpackViewModel.writeEditingProjectToDisk {
                            projectConfigRef.set(this, entry)
                        }
                        ModpackViewModel.loadFromDisk()
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        ContentBox(Modifier.padding(2.dp)) {
                            Text(label)
                        }
                    }
                    Row {
                        ImmediateTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        ModpackViewModel.writeEditingProjectToDisk {
                                            projectConfigRef.set(this, null)
                                        }
                                        ModpackViewModel.loadFromDisk()
                                        buttonState = modpackUiState.selectedProject?.let { projectRef(it) }
                                    }
                                }, modifier = Modifier.padding(horizontal = 4.dp).size(25.dp)
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback,
                                    "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

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
    else if (modpackUiState.selectedProject?.let { projectRef(it) } != null)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ContentBox(
                Modifier.padding(2.dp)
            ) {
                Text(label)
            }

            ContentBox(
                Modifier.padding(2.dp)
            ) {
                Text(projectRef(modpackUiState.selectedProject!!).name)
            }
        }
    }
}
