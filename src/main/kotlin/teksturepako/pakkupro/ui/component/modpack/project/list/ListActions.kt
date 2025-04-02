package teksturepako.pakkupro.ui.component.modpack.project.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkupro.ui.component.dialog.AddProjectsDialog
import teksturepako.pakkupro.ui.component.dialog.DismissibleDialog
import teksturepako.pakkupro.ui.component.modpack.project.ProjectCard
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListActions()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val selectedProjects = modpackUiState.selectedProjectsMap.values.size
    val projects = modpackUiState.lockFile?.get()?.getAllProjects()?.filter { project ->
        modpackUiState.selectedProjectsMap[project.pakkuId]?.let { it(project) } == true
    } ?: emptyList()

    Row(
        modifier = Modifier
            .height(70.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowRow(
            Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            var addDialogVisible by remember { mutableStateOf(false) }

            AddProjectsDialog(addDialogVisible, { addDialogVisible = false })

            DefaultButton(
                onClick = {
                    addDialogVisible = true
                },
                Modifier.align(Alignment.Bottom)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        key = AllIconsKeys.General.InlineAdd,
                        contentDescription = "teksturepako.pakkupro.actions.add",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Add Projects...")
                }
            }

            Spacer(
                Modifier
                    .background(JewelTheme.globalColors.borders.disabled)
                    .width(1.dp)
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight()
            )

            FlowColumn(
                Modifier.align(Alignment.Bottom),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row {
                    val message = when
                    {
                        modpackUiState.lockFile?.get()?.getAllProjects()?.size == selectedProjects ->
                        {
                            "All $selectedProjects projects selected"
                        }
                        selectedProjects > 1  -> "$selectedProjects projects selected"
                        selectedProjects == 1 -> "1 project selected"
                        else                  -> "0 projects selected"
                    }

                    Text(message)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            Text("Remove...")
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
    }
}