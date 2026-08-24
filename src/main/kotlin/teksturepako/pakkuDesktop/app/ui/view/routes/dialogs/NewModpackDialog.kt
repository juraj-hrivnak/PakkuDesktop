/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldOptions
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InitSpec
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.rememberListComboBoxState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewModpackDialog(
    profileData: ProfileData,
    onDismiss: () -> Unit,
    publish: (AppMsg) -> Unit,
) {
    val defaultName = profileData.currentProfile?.name.orEmpty().ifBlank { "Modpack" }
    val nameState = rememberTextFieldState(defaultName)
    val mcState = rememberTextFieldState("1.20.1")
    var loader by remember { mutableStateOf(ModpackFieldOptions.LOADERS.first()) }
    var target by remember { mutableStateOf(ModpackFieldOptions.TARGETS.first()) }

    fun submit() {
        publish(
            AppMsg.Modpack(
                ModpackMsg.InitRequested(
                    InitSpec(
                        name = nameState.text.toString().trim(),
                        mcVersion = mcState.text.toString().trim(),
                        loader = loader,
                        target = target,
                    ),
                ),
            ),
        )
    }

    PkUiDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "Modpack '${profileData.currentProfile?.name}' is not initialized.",
        onConfirm = { submit() },
        maxWidth = 480.dp,
    ) {
        Column(
            Modifier
                .padding(PakkuDesktopConstants.commonPaddingSize)
                .widthIn(min = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Create a new modpack in this folder.")

            GroupHeader("Basics")
            LabeledField("Name") {
                TextField(state = nameState, modifier = Modifier.fillMaxWidth())
            }
            LabeledField("Minecraft version") {
                TextField(state = mcState, modifier = Modifier.fillMaxWidth())
            }

            GroupHeader("Runtime")
            LabeledField("Loader") {
                ListComboBox(
                    items = ModpackFieldOptions.LOADERS,
                    selectedIndex = ModpackFieldOptions.LOADERS.indexOf(loader).coerceAtLeast(0),
                    onSelectedItemChange = { index ->
                        ModpackFieldOptions.LOADERS.getOrNull(index)?.let { loader = it }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    listState = rememberListComboBoxState(
                        ModpackFieldOptions.LOADERS,
                        ModpackFieldOptions.LOADERS.indexOf(loader).coerceAtLeast(0),
                    ),
                )
            }
            LabeledField("Target") {
                ListComboBox(
                    items = ModpackFieldOptions.TARGETS,
                    selectedIndex = ModpackFieldOptions.TARGETS.indexOf(target).coerceAtLeast(0),
                    onSelectedItemChange = { index ->
                        ModpackFieldOptions.TARGETS.getOrNull(index)?.let { target = it }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    listState = rememberListComboBoxState(
                        ModpackFieldOptions.TARGETS,
                        ModpackFieldOptions.TARGETS.indexOf(target).coerceAtLeast(0),
                    ),
                )
            }

            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedButton(
                    onClick = { submit() },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { Text("Create") }
                DefaultButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = JewelTheme.contentColor.copy(alpha = 0.75f))
        content()
    }
}
