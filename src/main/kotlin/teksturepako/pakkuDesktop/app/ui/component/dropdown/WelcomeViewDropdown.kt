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
import teksturepako.pakkuDesktop.app.ui.model.WelcomeDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

val welcomeDropdownComponent = component(
    init = WelcomeDropdownModel(),
    update = { _, model -> model },
    view = { publish, model ->
        val pickDirectory = LocalPickDirectory.current
        val dark = model.profileData.intUiTheme.isDark()

        PkUiDropdown(
            modifier = Modifier.padding(vertical = 4.dp),
            menuModifier = Modifier.width(240.dp),
            content = {
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Modpack")
                }
            },
            menuContent = {

                selectableItem(false, onClick = { pickDirectory() }) {
                    Row(
                        Modifier.fillMaxWidth().padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                key = PakkuDesktopIcons.open,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (dark) Color.White else Color.Black,
                            )
                            Text("Open...", color = if (dark) Color.White else Color.Black)
                        }
                        Text(acceleratorLabel("O"), color = Color.Gray)
                    }
                }

                selectableItem(false, onClick = { publish(WelcomeDropdownMsg.ShowSettings) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Settings...", color = if (dark) Color.White else Color.Black)
                        Text(acceleratorLabel(","), color = Color.Gray)
                    }
                }

                if (model.profileData.recentProfilesFiltered.isNotEmpty()) {
                    separator()

                    passiveItem {
                        Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                            Text("Recent Modpacks", color = Color.Gray)
                        }
                    }

                    model.profileData.recentProfilesFiltered.forEach { profile ->
                        selectableItem(false, onClick = { publish(WelcomeDropdownMsg.RecentProfile(profile.path)) }) {
                            Row {
                                Column(Modifier.fillMaxWidth(0.2f)) {}
                                Column {
                                    Text(profile.name, color = if (dark) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }
            },
        )
    },
)

// -- View --

@Composable
fun WelcomeViewDropdown(
    publish: (WelcomeMsg) -> Unit,
    model: WelcomeModel,
) {
    welcomeDropdownComponent.view({ publish(WelcomeMsg.WelcomeDropdown(it)) }, model.dropdown)
}
