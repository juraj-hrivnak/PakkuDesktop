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
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

@Composable
fun WelcomeViewDropdown(
    onOpenDirectory: () -> Unit,
    onNewModpack: () -> Unit,
    onRecentProfile: (String) -> Unit,
) {
    val profileData = LocalAppModel.current.profile.data

    PkUiDropdown(
        Modifier.padding(vertical = 4.dp),
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Modpack")
            }
        },
        menuModifier = Modifier.width(160.dp),
        menuContent = {

            // -- OPEN --
            selectableItem(false, onClick = { onOpenDirectory() }) {
                Row {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.open,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text("Open...", color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black)
                    }
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
                    selectableItem(false, onClick = { onRecentProfile(profile.path) }) {
                        Row {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column {
                                Text(profile.name, color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }
        }
    )
}
