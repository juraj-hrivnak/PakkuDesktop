/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import teksturepako.pakkuDesktop.pkui.component.TooltipPosition
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@Composable
fun FileHeader(diffContent: DiffContent)
{
    val file = Path(workingPath).resolve(diffContent.newPath)
    val parentDir = file.parent

    HorizontalBar {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f)) {
                SelectionContainer {
                    Column(Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = diffContent.newPath,
                            style = JewelTheme.defaultTextStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = diffFileTitleColor(),
                        )

                        if (diffContent.oldPath != null && diffContent.oldPath != diffContent.newPath)
                        {
                            Text(
                                text = "(was: ${diffContent.oldPath})",
                                style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace),
                                color = diffFileSubtitleColor(),
                            )
                        }
                    }
                }
            }

            PkUiTooltip(
                tooltip = { Text("Open Folder") },
                position = TooltipPosition.BOTTOM,
            ) {
                IconButton(
                    onClick = {
                        FileKit.openFileWithDefaultApplication(PlatformFile(parentDir.absolutePathString()))
                    },
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        key = AllIconsKeys.General.OpenDisk,
                        contentDescription = "Open folder",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                    )
                }
            }

            PkUiTooltip(
                tooltip = { Text("Open") },
                position = TooltipPosition.BOTTOM,
            ) {
                IconButton(
                    onClick = {
                        FileKit.openFileWithDefaultApplication(PlatformFile(file.absolutePathString()))
                    },
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Edit,
                        contentDescription = "Open",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                    )
                }
            }
        }
    }
}
