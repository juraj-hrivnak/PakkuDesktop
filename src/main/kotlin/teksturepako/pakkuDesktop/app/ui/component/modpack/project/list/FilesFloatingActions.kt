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
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesFabAction
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun BoxScope.FilesFloatingActions(publish: (ModpackMsg) -> Unit, model: FilesTabModel) {
    val busy = model.actionName != null
    val canRun = !busy && model.lockFile?.get() != null
    val scanning = model.actionName == "Scanning files"
    val detecting = model.actionName == "Detecting files"
    val lastFab = model.lastFilesFab

    val palette = JewelTheme.colorPalette
    val refreshAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor
    val syncAccent = palette.blue.getOrNull(palette.blue.lastIndex / 2)
        ?: PakkuDesktopConstants.highlightColor

    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Refresh))
                publish(ModpackMsg.FetchDetectRequested)
            },
            enabled = canRun,
            buttonSize = 44.dp,
            accent = refreshAccent,
            isDefault = lastFab == FilesFabAction.Refresh,
            tooltip = "Fetch: preview writing Pakku's files to disk",
        ) { hovered ->
            val accentHot = hovered || lastFab == FilesFabAction.Refresh || scanning
            Icon(
                key = AllIconsKeys.Actions.Download,
                contentDescription = if (scanning) "Scanning files" else "Fetch",
                tint = when {
                    scanning -> refreshAccent
                    !canRun -> Color.Gray
                    accentHot -> refreshAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }

        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.FilesFabRemembered(FilesFabAction.Sync))
                publish(ModpackMsg.SyncDetectRequested)
            },
            enabled = canRun,
            buttonSize = 44.dp,
            accent = syncAccent,
            isDefault = lastFab == FilesFabAction.Sync,
            tooltip = "Sync: preview reading disk changes into Pakku",
        ) { hovered ->
            val accentHot = hovered || lastFab == FilesFabAction.Sync || detecting
            Icon(
                key = AllIconsKeys.Actions.ListChanges,
                contentDescription = if (detecting) "Detecting files" else "Sync",
                tint = when {
                    detecting -> syncAccent
                    !canRun -> Color.Gray
                    accentHot -> syncAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
