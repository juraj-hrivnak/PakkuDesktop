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

        PkUiDropdown(
            modifier = Modifier.padding(vertical = 4.dp),
            menuModifier = Modifier.width(160.dp),
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

                // -- OPEN --
                selectableItem(false, onClick = { pickDirectory() }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(
                                key = PakkuDesktopIcons.open,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (model.profileData.intUiTheme.isDark()) Color.White else Color.Black,
                            )
                        }
                        Column {
                            Text("Open...", color = if (model.profileData.intUiTheme.isDark()) Color.White else Color.Black)
                        }
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
                                    Text(profile.name, color = if (model.profileData.intUiTheme.isDark()) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }
            },
        )
    },
)

// ---------------------------------------------------------------------------
// View entry point
// ---------------------------------------------------------------------------

@Composable
fun WelcomeViewDropdown(
    publish: (WelcomeMsg) -> Unit,
    model: WelcomeModel,
) {
    welcomeDropdownComponent.view({ publish(WelcomeMsg.WelcomeDropdown(it)) }, model.dropdown)
}
