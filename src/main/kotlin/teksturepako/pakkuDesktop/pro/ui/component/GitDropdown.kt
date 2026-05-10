/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

@Composable
fun GitDropdown(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val gitState = model.git

    var pushDialogVisible by remember { mutableStateOf(false) }

    GitPushDialog(
        publish = publish,
        model = model,
        visible = pushDialogVisible,
        onDismiss = { pushDialogVisible = false },
    )

    PkUiDropdown(
        Modifier.padding(vertical = 4.dp),
        content = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    key = AllIconsKeys.General.Vcs,
                    contentDescription = "Clone Repository Icon",
                    tint = JewelTheme.contentColor,
                    hints = arrayOf(),
                    modifier = Modifier.size(15.dp),
                )
                Text(gitState.branches.firstOrNull { it.isCurrent }?.name ?: "Git")
            }
        },
        menuModifier = Modifier
            .width(300.dp),
        menuContent = {
            selectableItem(false, onClick = {
                publish(ModpackMsg.GitPullRequested)
            }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Actions.CheckOut,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = JewelTheme.contentColor,
                        )
                    }
                    Column {
                        Text("Pull...", Modifier, color = JewelTheme.contentColor)
                    }
                }
            }

            selectableItem(false, onClick = {
                publish(ModpackMsg.TabSelected(SelectedTab.COMMIT))
            }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Actions.Commit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = JewelTheme.contentColor,
                        )
                    }
                    Column {
                        Text("Commit...", Modifier, color = JewelTheme.contentColor)
                    }
                }
            }

            selectableItem(false, onClick = {
                publish(ModpackMsg.GitPushRequested)
            }) {
                Row(Modifier.padding(2.dp)) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Vcs.Push,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = JewelTheme.contentColor,
                        )
                    }
                    Column {
                        val outgoingCommits = gitState.outgoingCommits.size
                        Text("Push... $outgoingCommits", Modifier, color = JewelTheme.contentColor)
                    }
                }
            }

            separator()

            passiveItem {
                Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                    Text("Local Branches", color = Color.Gray)
                }
            }

            gitState.branches.filterNot { it.isRemote }.forEach { branch ->
                selectableItem(false, onClick = {
                    publish(ModpackMsg.GitCheckoutRequested(branch))
                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {}
                        Column {
                            Text(branch.name, Modifier, color = JewelTheme.contentColor)
                        }
                    }
                }
            }

            passiveItem {
                Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                    Text("Remote Branches", color = Color.Gray)
                }
            }

            gitState.branches.filter { it.isRemote }.forEach { branch ->
                selectableItem(false, onClick = {
                    publish(ModpackMsg.GitCheckoutRequested(branch))
                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {}
                        Column {
                            Text(branch.name, Modifier, color = JewelTheme.contentColor)
                        }
                    }
                }
            }
        },
    )

    model.gitEventProgress?.let { event ->
        Text(event.operation)
        HorizontalProgressBar(event.percentage, Modifier.width(60.dp))
    }
}
