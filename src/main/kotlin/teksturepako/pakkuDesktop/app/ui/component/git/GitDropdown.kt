/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.menuStyle
import teksturepako.pakkuDesktop.app.git.GitBranch
import teksturepako.pakkuDesktop.app.git.GitDialog
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.application.titlebar.LocalTitleBarStartSlotMaxWidth
import teksturepako.pakkuDesktop.app.ui.application.titlebar.titleBarLabelMaxWidth
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown
import teksturepako.pakkuDesktop.pkui.component.pkUiSubmenu

// -- Component --

val gitDropdownComponent = component(
    init = GitDropdownModel(),
    // No local state: every message is handled by the parent.
    update = { _: GitDropdownMsg, model: GitDropdownModel -> model },
    view = { publish, model ->
        val gitState = model.gitState
        val enabled = !model.busy
        val current = gitState.currentBranch
        val branchName = current?.name ?: "Git"
        val labelMaxWidth = titleBarLabelMaxWidth(LocalTitleBarStartSlotMaxWidth.current, fraction = 0.28f)
        // [PkUiDropdown] menuContent is MenuScope.() -> Unit (not @Composable); read menu colors here.
        val menuItemColors = JewelTheme.menuStyle.colors.itemColors
        val muted = menuItemColors.keybindingTint
        val outgoing = gitState.outgoingCommits.size
        val incoming = gitState.incomingCount

        fun dialog(d: GitDialog) = publish(GitDropdownMsg.ShowDialog(d))
        fun op(o: GitOp) = publish(GitDropdownMsg.OpRequested(o))

        fun MenuScope.action(label: String, icon: IconKey?, isEnabled: Boolean = enabled, onClick: () -> Unit) {
            selectableItem(selected = false, iconKey = icon, onClick = onClick, enabled = isEnabled) {
                Text(label, color = menuItemColors.content)
            }
        }

        fun MenuScope.header(label: String) {
            passiveItem {
                Text(label, Modifier.padding(start = 10.dp, top = 2.dp), color = muted)
            }
        }

        fun MenuScope.branchMenu(branch: GitBranch) {
            pkUiSubmenu(
                enabled = enabled,
                iconKey = if (branch.isCurrent) AllIconsKeys.Actions.Checked else null,
                submenu = {
                    action("Checkout", AllIconsKeys.Actions.CheckOut, isEnabled = enabled && !branch.isCurrent) {
                        op(GitOp.Checkout(branch))
                    }
                    action("New Branch from '${branch.name}'…", AllIconsKeys.General.Add) {
                        dialog(GitDialog.NewBranch(startPoint = branch))
                    }
                    if (!branch.isRemote) {
                        separator()
                        action("Delete", AllIconsKeys.General.Delete, isEnabled = enabled && !branch.isCurrent) {
                            dialog(GitDialog.DeleteBranch(branch))
                        }
                    }
                },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        branch.name,
                        color = menuItemColors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (branch.isCurrent && gitState.upstream != null) {
                        Text("→ ${gitState.upstream}", color = muted, maxLines = 1)
                    }
                }
            }
        }

        fun MenuScope.tagMenu(ref: GitRef) {
            pkUiSubmenu(
                enabled = enabled,
                iconKey = AllIconsKeys.Nodes.Tag,
                submenu = {
                    action("Checkout", AllIconsKeys.Actions.CheckOut) {
                        op(GitOp.CheckoutRevision(ref.hash))
                    }
                    action("New Branch from '${ref.name}'…", AllIconsKeys.General.Add) {
                        dialog(GitDialog.NewBranch(startRevision = ref.hash))
                    }
                    separator()
                    action("Delete", AllIconsKeys.General.Delete) {
                        dialog(GitDialog.DeleteTag(ref.name))
                    }
                },
            ) {
                Text(
                    ref.name,
                    color = menuItemColors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        PkUiDropdown(
            modifier = Modifier.padding(vertical = 4.dp),
            menuModifier = Modifier.width(320.dp),
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        key = AllIconsKeys.General.Vcs,
                        contentDescription = null,
                        tint = JewelTheme.globalColors.text.normal,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        branchName,
                        color = JewelTheme.globalColors.text.normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = labelMaxWidth),
                    )
                    if (incoming > 0) Text("↓$incoming", color = JewelTheme.contentColor.copy(alpha = 0.6f))
                    if (outgoing > 0) Text("↑$outgoing", color = JewelTheme.contentColor.copy(alpha = 0.6f))
                }
            },
            menuContent = {
                if (gitState.isRepository == false) {
                    action("Initialize Repository…", AllIconsKeys.Vcs.Branch) {
                        dialog(GitDialog.Init)
                    }
                } else {
                    action(
                        if (incoming > 0) "Update Project… ($incoming incoming)" else "Update Project…",
                        AllIconsKeys.Actions.CheckOut,
                    ) { dialog(GitDialog.Update) }
                    action("Commit…", AllIconsKeys.Actions.Commit) {
                        publish(GitDropdownMsg.TabSelected(SelectedTab.COMMIT))
                        dialog(GitDialog.Commit)
                    }
                    action(
                        if (outgoing > 0) "Push… ($outgoing ahead)" else "Push…",
                        AllIconsKeys.Vcs.Push,
                        isEnabled = enabled && gitState.defaultRemote != null,
                    ) { dialog(GitDialog.Push) }
                    action("Fetch", AllIconsKeys.Vcs.Fetch, isEnabled = enabled && gitState.remotes.isNotEmpty()) {
                        op(GitOp.Fetch)
                    }
                    action("Refresh", AllIconsKeys.Actions.Refresh) { op(GitOp.Refresh) }

                    separator()

                    action("Source Control", AllIconsKeys.Vcs.Changelist, isEnabled = true) {
                        publish(GitDropdownMsg.TabSelected(SelectedTab.COMMIT))
                    }
                    action("New Branch…", AllIconsKeys.General.Add) { dialog(GitDialog.NewBranch()) }
                    action(
                        "Stash Changes…",
                        AllIconsKeys.Vcs.ShelveSilent,
                        isEnabled = enabled && gitState.gitFiles.isNotEmpty(),
                    ) { dialog(GitDialog.Stash) }
                    action(
                        if (gitState.stashes.isEmpty()) "Unstash Changes…" else "Unstash Changes… (${gitState.stashes.size})",
                        AllIconsKeys.Vcs.Unshelve,
                        isEnabled = enabled && gitState.stashes.isNotEmpty(),
                    ) { dialog(GitDialog.Unstash) }

                    val local = gitState.branches.filterNot { it.isRemote }.sortedWith(compareBy({ !it.isCurrent }, { it.name }))
                    val remote = gitState.branches.filter { it.isRemote && !it.name.endsWith("/HEAD") }.sortedBy { it.name }
                    val tags = gitState.refs.filter { it.type == GitRefType.Tag }
                        .sortedWith(compareByDescending<GitRef> { it.createdAt ?: kotlinx.datetime.Instant.DISTANT_PAST }.thenBy { it.name })

                    if (local.isNotEmpty()) {
                        separator()
                        header("Local Branches")
                        local.forEach { branchMenu(it) }
                    }
                    if (remote.isNotEmpty()) {
                        separator()
                        header("Remote Branches")
                        remote.forEach { branchMenu(it) }
                    }
                    if (tags.isNotEmpty()) {
                        separator()
                        passiveItem {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    publish(GitDropdownMsg.TagsToggled(!model.tagsExpanded))
                                },
                            ) {
                                Icon(
                                    key = if (model.tagsExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                                    contentDescription = null,
                                    tint = muted,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text("Tags", color = muted)
                            }
                        }
                        if (model.tagsExpanded) {
                            tags.forEach { tagMenu(it) }
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
