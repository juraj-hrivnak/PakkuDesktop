/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldOptions
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InitSpec
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize
import teksturepako.pakkuDesktop.pkui.component.rememberListComboBoxState

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

    val canSubmit = nameState.text.isNotBlank() && mcState.text.isNotBlank()

    PkUiDialog(
        onDismiss = onDismiss,
        title = "Create modpack",
        icon = AllIconsKeys.General.Add,
        subtitle = "'${profileData.currentProfile?.name}' is not a Pakku modpack yet. Create one in this folder.",
        size = PkUiDialogSize.Small,
        onConfirm = { if (canSubmit) submit() },
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Create",
                onPrimary = ::submit,
                primaryEnabled = canSubmit,
                onSecondary = onDismiss,
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = JewelTheme.contentColor.copy(alpha = 0.75f))
        content()
    }
}
