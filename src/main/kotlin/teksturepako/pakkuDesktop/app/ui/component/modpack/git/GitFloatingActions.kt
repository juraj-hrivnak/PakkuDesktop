/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Size
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FloatingActionIcon
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.GitFabAction
import teksturepako.pakkuDesktop.app.ui.model.GitTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun BoxScope.GitFloatingActions(publish: (ModpackMsg) -> Unit, model: GitTabModel) {
    val busy = model.gitBusy
    val canPull = !busy
    val canPush = !busy && model.git.outgoingCommits.isNotEmpty()
    val canCommit = !busy && model.git.selectedFiles.isNotEmpty()
    val lastFab = model.lastGitFab

    val palette = JewelTheme.colorPalette
    val pullAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor
    val pushAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor
    val commitAccent = palette.green.getOrNull(palette.green.lastIndex / 2)
        ?: PakkuDesktopConstants.green

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.GitFabRemembered(GitFabAction.Pull))
                publish(ModpackMsg.ShowGitPullDialog)
            },
            enabled = canPull,
            buttonSize = 44.dp,
            accent = pullAccent,
            isDefault = lastFab == GitFabAction.Pull,
            tooltip = "Pull",
        ) { hovered ->
            val accentHot = hovered || lastFab == GitFabAction.Pull
            Icon(
                key = AllIconsKeys.Actions.CheckOut,
                contentDescription = "Pull",
                tint = when {
                    !canPull -> Color.Gray
                    accentHot -> pullAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(Size(20)),
                modifier = Modifier.size(20.dp),
            )
        }

        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.GitFabRemembered(GitFabAction.Push))
                publish(ModpackMsg.GitDropdown(GitDropdownMsg.ShowPushDialog))
            },
            enabled = canPush,
            buttonSize = 44.dp,
            accent = pushAccent,
            isDefault = lastFab == GitFabAction.Push,
            tooltip = "Push",
        ) { hovered ->
            val accentHot = hovered || lastFab == GitFabAction.Push
            Icon(
                key = AllIconsKeys.Vcs.Push,
                contentDescription = "Push",
                tint = when {
                    !canPush -> Color.Gray
                    accentHot -> pushAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(Size(20)),
                modifier = Modifier.size(20.dp),
            )
        }

        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.GitFabRemembered(GitFabAction.Commit))
                publish(ModpackMsg.ShowGitCommitDialog)
            },
            enabled = canCommit,
            buttonSize = 44.dp,
            accent = commitAccent,
            isDefault = lastFab == GitFabAction.Commit,
            tooltip = "Commit selected files",
        ) { hovered ->
            val accentHot = hovered || lastFab == GitFabAction.Commit
            Icon(
                key = AllIconsKeys.Actions.Commit,
                contentDescription = "Commit",
                tint = when {
                    !canCommit -> Color.Gray
                    accentHot -> commitAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
