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
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.CloseDialogRequest
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

@Composable
fun ModpackDropdown(
    publish: (AppMsg) -> Unit,
    model: AppModel,
    onOpenDirectory: () -> Unit,
    enabled: Boolean = true,
) {
    val profileData = model.profile.data
    val modpack = model.modpack

    PkUiDropdown(
        Modifier.padding(vertical = 4.dp),
        enabled = enabled,
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                profileData.currentProfile?.name?.let { Text(it) } ?: Text("Modpack")
            }
        },
        menuModifier = Modifier.width(200.dp),
        menuContent = {

            // -- OPEN --
            selectableItem(false, onClick = { onOpenDirectory() }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.open,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column { Text("Open...", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                }
            }

            // -- CLOSE --
            selectableItem(false, onClick = {
                if (modpack.actionName != null) {
                    publish(AppMsg.RequestCloseDialog(CloseDialogRequest.CloseModpack()))
                } else {
                    publish(AppMsg.NavigateToWelcome)
                }
            }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) { }
                    Column { Text("Close", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                }
            }

            separator()

            // -- EXPORT --
            selectableItem(
                selected = false,
                onClick = { publish(AppMsg.Modpack.ExportRequested) },
                enabled = modpack.actionName == null,
            ) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.cube,
                            "export",
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column { Text("Export", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                }
            }

            separator()

            // -- FETCH --
            selectableItem(false, onClick = { }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.cloudDownload,
                            contentDescription = "fetch",
                            Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column { Text("Fetch", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                }
            }

            if (profileData.recentProfilesFiltered.isNotEmpty()) {
                separator()

                passiveItem {
                    Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Text("Recent Modpacks", color = Color.Gray)
                    }
                }

                profileData.recentProfilesFiltered.forEach { profile ->
                    selectableItem(false, onClick = {
                        if (modpack.actionName != null) {
                            publish(AppMsg.RequestCloseDialog(CloseDialogRequest.OpenDirectory(profile.path)))
                        } else {
                            publish(AppMsg.DirectoryPicked(profile.path))
                        }
                    }) {
                        Row(Modifier.padding(2.dp)) {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column { Text(profile.name, color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                        }
                    }
                }
            }
        }
    )
}
