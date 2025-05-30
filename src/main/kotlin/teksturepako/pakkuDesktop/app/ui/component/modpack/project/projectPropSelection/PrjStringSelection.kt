/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import kotlin.reflect.KFunction1
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NullableProjectStringSelection(
    label: String,
    projectRef: KFunction1<Project, Result<String, ActionError>?>,
    projectConfigRef: KMutableProperty1<ConfigFile.ProjectConfig, String?>,
)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (modpackUiState.editingProject)
    {
        val textFieldState = rememberTextFieldState(
            modpackUiState.selectedProject?.let { projectRef(it) }?.get() ?: ""
        )

        LaunchedEffect(textFieldState.text)
        {
            coroutineScope.launch {
                if (textFieldState.text.toString().isNotBlank())
                {
                    ModpackViewModel.writeEditingProjectToDisk {
                        projectConfigRef.set(this, textFieldState.text.toString())
                    }
                }
                else
                {
                    ModpackViewModel.writeEditingProjectToDisk {
                        projectConfigRef.set(this, null)
                    }
                }
                ModpackViewModel.loadFromDisk()
            }
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
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        ModpackViewModel.writeEditingProjectToDisk {
                                            projectConfigRef.set(this, null)
                                        }
                                        ModpackViewModel.loadFromDisk()
                                        textFieldState.edit { this.delete(0, this.length) }
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

                FlowRow(
                    modifier = Modifier.padding(4.dp)
                ) {
                    TextField(textFieldState)
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

            projectRef(modpackUiState.selectedProject!!)
                ?.onSuccess {
                    ContentBox(
                        Modifier.padding(2.dp)
                    ) {
                        Text(it)
                    }
                }
                ?.onFailure {
                    ContentBox(
                        Modifier.padding(2.dp),
                        color = Color.Red
                    ) {
                        Text("Error: " + it.rawMessage)
                    }
                }
        }
    }
}
