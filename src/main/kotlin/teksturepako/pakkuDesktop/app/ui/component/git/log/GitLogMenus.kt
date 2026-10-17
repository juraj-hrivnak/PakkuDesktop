/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.ContextMenuItem
import org.jetbrains.jewel.ui.component.ContextMenuItemOption
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.git.GitBranch
import teksturepako.pakkuDesktop.app.git.GitDialog
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.log.GitLogGraph
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

fun gitLogCommitMenu(
    graph: GitLogGraph,
    row: Int,
    busy: Boolean,
    publish: (ModpackMsg) -> Unit,
    copy: (String) -> Unit,
    jumpTo: (Int) -> Unit,
): List<ContextMenuItem> {
    val hash = graph.hashAt(row)
    val merge = graph.parentCount(row) > 1
    val parent = graph.parentRow(row)
    val child = graph.childRow(row)
    return listOf(
        ContextMenuItemOption(
            label = "Copy Revision Number",
            icon = AllIconsKeys.Actions.Copy,
            actionType = ContextMenuItemOptionAction.CopyMenuItemOptionAction,
            action = { copy(hash) },
        ),
        ContextMenuItemOption(
            label = "Checkout Revision",
            icon = AllIconsKeys.Actions.CheckOut,
            enabled = !busy,
            action = { publish(ModpackMsg.GitOpRequested(GitOp.CheckoutRevision(hash))) },
        ),
        ContextMenuItemOption(
            label = "Cherry-Pick",
            icon = AllIconsKeys.Vcs.Merge,
            enabled = !busy,
            action = { publish(ModpackMsg.GitOpRequested(GitOp.CherryPick(hash, merge))) },
        ),
        ContextMenuItemOption(
            label = "Revert Commit",
            icon = AllIconsKeys.Actions.Rollback,
            enabled = !busy,
            action = { publish(ModpackMsg.GitOpRequested(GitOp.Revert(hash, merge))) },
        ),
        ContextMenuItemOption(
            label = "Reset Current Branch to Here…",
            icon = AllIconsKeys.Actions.Rollback,
            enabled = !busy,
            action = { publish(ModpackMsg.ShowGitDialog(GitDialog.Reset(hash))) },
        ),
        ContextMenuItemOption(
            label = "New Branch…",
            icon = AllIconsKeys.General.Add,
            enabled = !busy,
            action = { publish(ModpackMsg.ShowGitDialog(GitDialog.NewBranch(startRevision = hash))) },
        ),
        ContextMenuItemOption(
            label = "New Tag…",
            icon = AllIconsKeys.Nodes.Tag,
            enabled = !busy,
            action = { publish(ModpackMsg.ShowGitDialog(GitDialog.NewTag(hash))) },
        ),
        ContextMenuItemOption(
            label = "Go to Parent Commit",
            icon = AllIconsKeys.Actions.Find,
            enabled = parent != null,
            action = { parent?.let(jumpTo) },
        ),
        ContextMenuItemOption(
            label = "Go to Child Commit",
            icon = AllIconsKeys.Actions.Find,
            enabled = child != null,
            action = { child?.let(jumpTo) },
        ),
    )
}

fun gitLogBranchMenu(
    branch: GitBranch,
    busy: Boolean,
    publish: (ModpackMsg) -> Unit,
): List<ContextMenuItem> = listOf(
    ContextMenuItemOption(
        label = "Checkout",
        icon = AllIconsKeys.Actions.CheckOut,
        enabled = !busy && !branch.isCurrent,
        action = { publish(ModpackMsg.GitOpRequested(GitOp.Checkout(branch))) },
    ),
    ContextMenuItemOption(
        label = "New Branch from '${branch.name}'…",
        icon = AllIconsKeys.General.Add,
        enabled = !busy,
        action = { publish(ModpackMsg.ShowGitDialog(GitDialog.NewBranch(startPoint = branch))) },
    ),
    ContextMenuItemOption(
        label = "Delete",
        icon = AllIconsKeys.General.Delete,
        enabled = !busy && !branch.isRemote && !branch.isCurrent,
        action = { publish(ModpackMsg.ShowGitDialog(GitDialog.DeleteBranch(branch))) },
    ),
)

fun gitLogTagMenu(
    ref: GitRef,
    busy: Boolean,
    publish: (ModpackMsg) -> Unit,
): List<ContextMenuItem> = listOf(
    ContextMenuItemOption(
        label = "Checkout Tag",
        icon = AllIconsKeys.Actions.CheckOut,
        enabled = !busy,
        action = { publish(ModpackMsg.GitOpRequested(GitOp.CheckoutRevision(ref.hash))) },
    ),
    ContextMenuItemOption(
        label = "New Branch from '${ref.name}'…",
        icon = AllIconsKeys.General.Add,
        enabled = !busy,
        action = { publish(ModpackMsg.ShowGitDialog(GitDialog.NewBranch(startRevision = ref.hash))) },
    ),
    ContextMenuItemOption(
        label = "Delete",
        icon = AllIconsKeys.General.Delete,
        enabled = !busy,
        action = { publish(ModpackMsg.ShowGitDialog(GitDialog.DeleteTag(ref.name))) },
    ),
)
