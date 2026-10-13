/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

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
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesFabAction
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SyncKind

@Composable
fun BoxScope.FilesPreviewFloatingActions(publish: (ModpackMsg) -> Unit, model: FilesTabModel) {
    val lastFab = model.lastFilesFab
    val fetching = model.actionName == "Fetching" ||
        model.actionName == "Syncing manual overrides" ||
        model.actionName == "Deleting old files"
    val applying = model.actionName == "Syncing"
    val palette = JewelTheme.colorPalette
    val fetchAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor
    val addAccent = palette.green.getOrNull(palette.green.lastIndex / 2)
        ?: PakkuDesktopConstants.green
    val updateAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor
    val removeAccent = palette.red.getOrNull(palette.red.lastIndex / 2)
        ?: PakkuDesktopConstants.coral

    val showFetch = model.filesPreviewMode == FilesPreviewMode.Fetch
    val showSync = model.filesPreviewMode == FilesPreviewMode.Sync
    val canFetch = model.canApplyFetch()
    val canAdd = model.canApplySync(SyncKind.Added)
    val canUpdate = model.canApplySync(SyncKind.Updated)
    val canRemove = model.canApplySync(SyncKind.Removed)

    if (!showFetch && !showSync) return

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFetch) {
            FloatingActionIcon(
                onClick = {
                    publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Fetch))
                    publish(ModpackMsg.FetchRequested)
                },
                enabled = canFetch,
                buttonSize = 44.dp,
                accent = fetchAccent,
                isDefault = lastFab == FilesFabAction.Fetch,
            ) { hovered ->
                val accentHot = hovered || lastFab == FilesFabAction.Fetch || fetching
                Icon(
                    key = AllIconsKeys.Actions.Checked,
                    contentDescription = if (fetching) "Fetching" else "Apply to file system",
                    tint = when {
                        !canFetch && !fetching -> Color.Gray
                        accentHot -> fetchAccent
                        else -> JewelTheme.contentColor
                    },
                    hints = arrayOf(),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (showSync) {
            if (model.syncAdded.isNotEmpty()) {
                FloatingActionIcon(
                    onClick = {
                        publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Add))
                        publish(ModpackMsg.SyncApplyRequested(SyncKind.Added))
                    },
                    enabled = canAdd,
                    buttonSize = 44.dp,
                    accent = addAccent,
                    isDefault = lastFab == FilesFabAction.Add,
                ) { hovered ->
                    Icon(
                        key = AllIconsKeys.General.InlineAdd,
                        contentDescription = "Add to Pakku",
                        tint = when {
                            !canAdd -> Color.Gray
                            hovered || lastFab == FilesFabAction.Add || applying -> addAccent
                            else -> JewelTheme.contentColor
                        },
                        hints = arrayOf(),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (model.syncUpdated.isNotEmpty()) {
                FloatingActionIcon(
                    onClick = {
                        publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Update))
                        publish(ModpackMsg.SyncApplyRequested(SyncKind.Updated))
                    },
                    enabled = canUpdate,
                    buttonSize = 44.dp,
                    accent = updateAccent,
                    isDefault = lastFab == FilesFabAction.Update,
                ) { hovered ->
                    Icon(
                        key = AllIconsKeys.Actions.CheckOut,
                        contentDescription = "Update in Pakku",
                        tint = when {
                            !canUpdate -> Color.Gray
                            hovered || lastFab == FilesFabAction.Update || applying -> updateAccent
                            else -> JewelTheme.contentColor
                        },
                        hints = arrayOf(),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (model.syncRemoved.isNotEmpty()) {
                FloatingActionIcon(
                    onClick = {
                        publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Remove))
                        publish(ModpackMsg.SyncApplyRequested(SyncKind.Removed))
                    },
                    enabled = canRemove,
                    buttonSize = 44.dp,
                    accent = removeAccent,
                    isDefault = lastFab == FilesFabAction.Remove,
                ) { hovered ->
                    Icon(
                        key = AllIconsKeys.General.Delete,
                        contentDescription = "Remove from Pakku",
                        tint = when {
                            !canRemove -> Color.Gray
                            hovered || lastFab == FilesFabAction.Remove || applying -> removeAccent
                            else -> JewelTheme.contentColor
                        },
                        hints = arrayOf(),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
