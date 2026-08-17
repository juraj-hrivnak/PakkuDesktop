/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.acceleratorLabel
import teksturepako.pakkuDesktop.app.ui.driver.LocalPickDirectory
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

// -- Component --

val modpackDropdownComponent = component<ModpackDropdownModel, ModpackDropdownMsg>(
    init = ModpackDropdownModel(),
    update = { msg, model ->
        when (msg) {
            is ModpackDropdownMsg.CloseRequested,
            ModpackDropdownMsg.Export,
            ModpackDropdownMsg.Fetch,
            ModpackDropdownMsg.CheckUpdates,
            ModpackDropdownMsg.ShowSettings,
            is ModpackDropdownMsg.DirectoryPicked -> model
        }
    },
    view = { publish, model ->
        val pickDirectory = LocalPickDirectory.current
        val dark = model.profileData.intUiTheme.isDark()

        PkUiDropdown(
            modifier = Modifier.padding(vertical = 4.dp),
            enabled = model.enabled,
            menuModifier = Modifier.width(260.dp),
            content = {
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    model.profileData.currentProfile?.name?.let { Text(it) } ?: Text("Modpack")
                }
            },
            menuContent = {

                selectableItem(false, onClick = { pickDirectory() }) {
                    DropdownRow(
                        icon = {
                            Icon(
                                key = PakkuDesktopIcons.open,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (dark) Color.White else Color.Black,
                            )
                        },
                        label = "Open...",
                        shortcut = acceleratorLabel("O"),
                        dark = dark,
                    )
                }

                selectableItem(false, onClick = { publish(ModpackDropdownMsg.CloseRequested()) }) {
                    DropdownRow(
                        label = "Close",
                        shortcut = acceleratorLabel("W"),
                        dark = dark,
                    )
                }

                separator()

                selectableItem(false, onClick = { publish(ModpackDropdownMsg.ShowSettings) }) {
                    DropdownRow(
                        label = "Settings...",
                        shortcut = acceleratorLabel(","),
                        dark = dark,
                    )
                }

                separator()

                selectableItem(
                    selected = false,
                    onClick = { publish(ModpackDropdownMsg.Export) },
                    enabled = model.actionEnabled,
                ) {
                    DropdownRow(
                        icon = {
                            Icon(
                                key = PakkuDesktopIcons.cube,
                                "export",
                                modifier = Modifier.size(15.dp),
                                tint = if (dark) Color.White else Color.Black,
                            )
                        },
                        label = "Export",
                        shortcut = acceleratorLabel("E"),
                        dark = dark,
                    )
                }

                separator()

                selectableItem(
                    selected = false,
                    onClick = { publish(ModpackDropdownMsg.Fetch) },
                    enabled = model.actionEnabled,
                ) {
                    DropdownRow(
                        icon = {
                            Icon(
                                key = PakkuDesktopIcons.cloudDownload,
                                contentDescription = "fetch",
                                Modifier.size(15.dp),
                                tint = if (dark) Color.White else Color.Black,
                            )
                        },
                        label = "Fetch",
                        shortcut = acceleratorLabel("Shift+F"),
                        dark = dark,
                    )
                }

                selectableItem(
                    selected = false,
                    onClick = { publish(ModpackDropdownMsg.CheckUpdates) },
                    enabled = model.actionEnabled,
                ) {
                    DropdownRow(
                        label = "Check for updates",
                        dark = dark,
                    )
                }

                if (model.profileData.recentProfilesFiltered.isNotEmpty()) {
                    separator()

                    passiveItem {
                        Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                            Text("Recent Modpacks", color = Color.Gray)
                        }
                    }

                    model.profileData.recentProfilesFiltered.forEach { profile ->
                        selectableItem(false, onClick = { publish(ModpackDropdownMsg.DirectoryPicked(profile.path)) }) {
                            DropdownRow(
                                label = profile.name,
                                dark = dark,
                            )
                        }
                    }
                }
            },
        )
    },
)

@Composable
private fun DropdownRow(
    label: String,
    dark: Boolean,
    shortcut: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val color = if (dark) Color.White else Color.Black
    Row(
        Modifier.fillMaxWidth().padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                icon?.invoke()
            }
            Text(label, color = color)
        }
        shortcut?.let {
            Text(it, color = Color.Gray)
        }
    }
}

// -- View --

@Composable
fun ModpackDropdown(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    modpackDropdownComponent.view({ publish(ModpackMsg.ModpackDropdown(it)) }, model.modpackDropdown)
}
