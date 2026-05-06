/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.PropertyWrite
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import kotlin.enums.EnumEntries
import kotlin.reflect.KMutableProperty1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T : Enum<T>> NullableProjectEnumSelection(
    label: String,
    enumEntries: EnumEntries<T>,
    projectRef: KMutableProperty1<Project, T?>,
    projectConfigRef: KMutableProperty1<ConfigFile.ProjectConfig, T?>,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    if (model.editingProject) {
        var buttonState by remember { mutableStateOf(model.selectedProject?.let { projectRef(it) }) }

        val buttons = enumEntries.map { entry ->
            SegmentedControlButtonData(
                selected = buttonState == entry,
                content = { _ -> Text(entry.name) },
                onSelect = {
                    buttonState = entry
                    publish(ModpackMsg.PropertyWriteRequested(PropertyWrite { projectConfigRef.set(this, entry) }))
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
                    Row {
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    publish(ModpackMsg.PropertyWriteRequested(PropertyWrite { projectConfigRef.set(this, null) }))
                                    buttonState = model.selectedProject?.let { projectRef(it) }
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp)
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback, "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
                FlowRow(modifier = Modifier.padding(2.dp)) {
                    buttons.forEach { button -> SegmentedControl(listOf(button), Modifier.padding(2.dp)) }
                }
            }
        }
    } else if (model.selectedProject?.let { projectRef(it) } != null) {
        FlowRow(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            ContentBox(Modifier.padding(2.dp)) { Text(label) }
            ContentBox(Modifier.padding(2.dp)) { Text(projectRef(model.selectedProject)!!.name) }
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
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    if (model.editingProject) {
        var buttonState by remember { mutableStateOf(model.selectedProject?.let { projectRef(it) }) }

        val buttons = enumEntries.map { entry ->
            SegmentedControlButtonData(
                selected = buttonState == entry,
                content = { _ -> Text(entry.name) },
                onSelect = {
                    buttonState = entry
                    publish(ModpackMsg.PropertyWriteRequested(PropertyWrite { projectConfigRef.set(this, entry) }))
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
                    Row {
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    publish(ModpackMsg.PropertyWriteRequested(PropertyWrite { projectConfigRef.set(this, null) }))
                                    buttonState = model.selectedProject?.let { projectRef(it) }
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp)
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback, "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
                FlowRow(modifier = Modifier.padding(2.dp)) {
                    buttons.forEach { button -> SegmentedControl(listOf(button), Modifier.padding(2.dp)) }
                }
            }
        }
    } else if (model.selectedProject?.let { projectRef(it) } != null) {
        FlowRow(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            ContentBox(Modifier.padding(2.dp)) { Text(label) }
            ContentBox(Modifier.padding(2.dp)) { Text(projectRef(model.selectedProject!!).name) }
        }
    }
}
