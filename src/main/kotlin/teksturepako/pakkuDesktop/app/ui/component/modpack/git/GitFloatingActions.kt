/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.git

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import teksturepako.pakkuDesktop.app.git.GitDialog
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FloatingActionIcon
import teksturepako.pakkuDesktop.app.ui.model.GitFabAction
import teksturepako.pakkuDesktop.app.ui.model.GitTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun BoxScope.GitFloatingActions(publish: (ModpackMsg) -> Unit, model: GitTabModel) {
    val canCommit = !model.gitBusy && model.git.selectedFiles.isNotEmpty()
    val commitAccent = PakkuDesktopConstants.highlightColor

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
    ) {
        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.GitFabRemembered(GitFabAction.Commit))
                publish(ModpackMsg.ShowGitDialog(GitDialog.Commit))
            },
            enabled = canCommit,
            buttonSize = 44.dp,
            accent = commitAccent,
            tooltip = "Commit selected files (Ctrl+K)",
        ) { hovered ->
            Icon(
                key = AllIconsKeys.Actions.Commit,
                contentDescription = "Commit",
                tint = when {
                    !canCommit -> Color.Gray
                    hovered -> commitAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
