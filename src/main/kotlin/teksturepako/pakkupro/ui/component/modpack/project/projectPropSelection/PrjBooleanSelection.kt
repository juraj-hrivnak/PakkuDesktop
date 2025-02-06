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
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectBooleanSelection(
    label: String,
    projectRef: KMutableProperty1<Project, Boolean>,
    projectConfigRef: KMutableProperty1<ConfigFile.ProjectConfig, Boolean?>,
)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (modpackUiState.editingProject)
    {
        var buttonState by remember { mutableStateOf(modpackUiState.selectedProject?.let { projectRef(it) }) }

        val buttons = listOf(true, false).map { boolean ->
            SegmentedControlButtonData(
                selected = buttonState == boolean,
                content = { _ ->
                    Text(boolean.toString())
                },
                onSelect = {
                    buttonState = boolean

                    coroutineScope.launch {
                        ModpackViewModel.writeEditingProjectToDisk {
                            projectConfigRef.set(this, boolean)
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
                Text(projectRef(modpackUiState.selectedProject!!).toString())
            }
        }
    }
}
