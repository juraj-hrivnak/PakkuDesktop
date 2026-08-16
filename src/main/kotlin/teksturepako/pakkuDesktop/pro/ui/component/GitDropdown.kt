/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.menuStyle
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown

// -- Component --

val gitDropdownComponent = component(
    init = GitDropdownModel(),
    update = { msg, model ->
        when (msg) {
            GitDropdownMsg.ShowPushDialog -> model.copy(pushDialogVisible = true)
            GitDropdownMsg.HidePushDialog -> model.copy(pushDialogVisible = false)
            // parent
            GitDropdownMsg.PullRequested,
            GitDropdownMsg.PushRequested,
            is GitDropdownMsg.TabSelected,
            is GitDropdownMsg.CheckoutRequested -> model
        }
    },
    view = { publish, model ->
        val gitState = model.gitState
        // [PkUiDropdown] menuContent is MenuScope.() -> Unit (not @Composable); read menu colors here.
        val menuItemColors = JewelTheme.menuStyle.colors.itemColors

        GitPushDialog(
            gitState = gitState,
            visible = model.pushDialogVisible,
            onDismiss = { publish(GitDropdownMsg.HidePushDialog) },
            onPush = { publish(GitDropdownMsg.PushRequested) },
        )

        PkUiDropdown(
            modifier = Modifier.padding(vertical = 4.dp),
            menuModifier = Modifier.width(300.dp),
            content = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        key = AllIconsKeys.General.Vcs,
                        contentDescription = "Clone Repository Icon",
                        tint = JewelTheme.globalColors.text.normal,
                        hints = arrayOf(),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(gitState.branches.firstOrNull { it.isCurrent }?.name ?: "Git", color = JewelTheme.globalColors.text.normal)
                }
            },
            menuContent = {
                selectableItem(false, onClick = { publish(GitDropdownMsg.PullRequested) }) {
                    Row(Modifier.padding(2.dp)) {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(key = AllIconsKeys.Actions.CheckOut, contentDescription = null, modifier = Modifier.size(15.dp), tint = menuItemColors.iconTint)
                        }
                        Column { Text("Pull...", Modifier, color = menuItemColors.content) }
                    }
                }

                selectableItem(false, onClick = { publish(GitDropdownMsg.TabSelected(SelectedTab.COMMIT)) }) {
                    Row(Modifier.padding(2.dp)) {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(key = AllIconsKeys.Actions.Commit, contentDescription = null, modifier = Modifier.size(15.dp), tint = menuItemColors.iconTint)
                        }
                        Column { Text("Source control\u2026", Modifier, color = menuItemColors.content) }
                    }
                }

                selectableItem(false, onClick = { publish(GitDropdownMsg.ShowPushDialog) }) {
                    Row(Modifier.padding(2.dp)) {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(key = AllIconsKeys.Vcs.Push, contentDescription = null, modifier = Modifier.size(15.dp), tint = menuItemColors.iconTint)
                        }
                        Column {
                            val outgoing = gitState.outgoingCommits.size
                            Text(if (outgoing > 0) "Push\u2026 ($outgoing ahead)" else "Push\u2026", Modifier, color = menuItemColors.content)
                        }
                    }
                }

                separator()

                passiveItem {
                    Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Text("Local Branches", color = menuItemColors.keybindingTint)
                    }
                }

                gitState.branches.filterNot { it.isRemote }.forEach { branch ->
                    selectableItem(false, onClick = { publish(GitDropdownMsg.CheckoutRequested(branch)) }) {
                        Row {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column { Text(branch.name, Modifier, color = menuItemColors.content) }
                        }
                    }
                }

                passiveItem {
                    Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Text("Remote Branches", color = menuItemColors.keybindingTint)
                    }
                }

                gitState.branches.filter { it.isRemote }.forEach { branch ->
                    selectableItem(false, onClick = { publish(GitDropdownMsg.CheckoutRequested(branch)) }) {
                        Row {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column { Text(branch.name, Modifier, color = menuItemColors.content) }
                        }
                    }
                }
            },
        )
    },
)

// -- View --

@Composable
fun GitDropdown(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    gitDropdownComponent.view({ publish(ModpackMsg.GitDropdown(it)) }, model.gitDropdown)

    model.gitEventProgress?.let { event ->
        Text(event.operation)
        HorizontalProgressBar(event.percentage, Modifier.width(60.dp))
    }
}
