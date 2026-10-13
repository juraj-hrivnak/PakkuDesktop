/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.context

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.ContextMenuItemOption
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.git.GitChange
import teksturepako.pakkuDesktop.app.git.GitDialog
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.integration.revealInFileManager
import teksturepako.pakkuDesktop.app.integration.writeClipboardText
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiContextMenuArea

private fun selectGitFileOnly(file: GitFile, publish: (ModpackMsg) -> Unit) {
    publish(ModpackMsg.GitClearChangedFileSelection)
    publish(ModpackMsg.GitFileSelectionToggled(file))
}

fun gitFileContextMenuItems(
    file: GitFile,
    publish: (ModpackMsg) -> Unit,
    busy: Boolean,
    modpackRoot: Path?,
    copyToClipboard: (String) -> Unit,
): List<ContextMenuItem> {
    val isUntracked = file.status is GitChange.Untracked
    val diskPath = modpackRoot?.resolve(file.path.replace('\\', '/'))
    return buildList {
        add(
            ContextMenuItemOption(
                label = "Open diff",
                icon = AllIconsKeys.Actions.Preview,
                enabled = !busy,
                action = {
                    selectGitFileOnly(file, publish)
                    publish(ModpackMsg.GitDiffFileSelected(file))
                },
            ),
        )
        add(
            ContextMenuItemOption(
                label = "Copy path",
                icon = AllIconsKeys.Actions.Copy,
                actionType = ContextMenuItemOptionAction.CopyMenuItemOptionAction,
                action = { copyToClipboard(file.path) },
            ),
        )
        if (diskPath != null) {
            add(
                ContextMenuItemOption(
                    label = "Show in folder",
                    icon = AllIconsKeys.General.OpenDisk,
                    action = { revealInFileManager(diskPath) },
                ),
            )
        }
        if (isUntracked) {
            add(
                ContextMenuItemOption(
                    label = "Add to .gitignore",
                    icon = AllIconsKeys.Vcs.Ignore_file,
                    enabled = !busy,
                    action = {
                        selectGitFileOnly(file, publish)
                        publish(ModpackMsg.GitOpRequested(GitOp.Ignore(listOf(file.path))))
                    },
                ),
            )
        } else {
            add(
                ContextMenuItemOption(
                    label = "Rollback…",
                    icon = AllIconsKeys.Actions.Rollback,
                    enabled = !busy,
                    action = {
                        selectGitFileOnly(file, publish)
                        publish(ModpackMsg.ShowGitDialog(GitDialog.Rollback))
                    },
                ),
            )
        }
    }
}

fun gitFolderContextMenuItems(
    folderPath: String,
    expanded: Boolean,
    publish: (ModpackMsg) -> Unit,
): List<ContextMenuItem> = buildList {
    add(
        ContextMenuItemOption(
            label = if (expanded) "Collapse folder" else "Expand folder",
            icon = if (expanded) AllIconsKeys.Actions.Collapseall else AllIconsKeys.Actions.Expandall,
            action = { publish(ModpackMsg.GitChangelistFolderExpansionToggled(folderPath)) },
        ),
    )
    add(
        ContextMenuItemOption(
            label = "Select folder",
            icon = AllIconsKeys.Actions.Selectall,
            action = { publish(ModpackMsg.GitFolderSelectionToggled(folderPath)) },
        ),
    )
}

@Composable
fun GitFileRowContextMenu(
    file: GitFile,
    publish: (ModpackMsg) -> Unit,
    busy: Boolean,
    modpackRoot: Path?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val copy: (String) -> Unit = { text ->
        scope.launch(Dispatchers.IO) { writeClipboardText(text) }
    }
    PkUiContextMenuArea(
        modifier = modifier,
        items = { gitFileContextMenuItems(file, publish, busy, modpackRoot, copy) },
        content = { Box(Modifier.fillMaxWidth()) { content() } },
    )
}

@Composable
fun GitFolderRowContextMenu(
    folderPath: String,
    expanded: Boolean,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PkUiContextMenuArea(
        modifier = modifier,
        items = { gitFolderContextMenuItems(folderPath, expanded, publish) },
        content = { Box(Modifier.fillMaxWidth()) { content() } },
    )
}
