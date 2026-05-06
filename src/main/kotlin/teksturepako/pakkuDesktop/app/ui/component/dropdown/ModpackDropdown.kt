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
import teksturepako.pakkuDesktop.app.ui.LocalAppModel
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

@Composable
fun ModpackDropdown(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    onOpenDirectory: () -> Unit,
    enabled: Boolean = true,
) {
    // Profile data comes from the app-level LocalAppModel (provided by appComponent.view)
    val profileData = LocalAppModel.current.profile.data

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

            // -- CLOSE —
            // parent handles: if action running → close dialog; else → navigate
            selectableItem(false, onClick = { publish(ModpackMsg.CloseRequested()) }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) { }
                    Column { Text("Close", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black) }
                }
            }

            separator()

            // -- EXPORT --
            selectableItem(
                selected = false,
                onClick = { publish(ModpackMsg.ExportRequested) },
                enabled = model.actionName == null,
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
                        // parent handles: if action running → close dialog; else → set pendingPath
                        publish(ModpackMsg.DirectoryPicked(profile.path))
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
