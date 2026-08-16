/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.elm.animatedColor
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import teksturepako.pakkuDesktop.pro.ui.component.Pro

@Composable
fun ModpackSideBar(
    publish: (ModpackMsg) -> Unit,
    modpack: ModpackModel,
    appModel: AppModel,
) {
    val panelBackground = animatedColor(JewelTheme.globalColors.panelBackground)
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)
    Row {
        Column(
            Modifier
                .fillMaxHeight()
                .width(48.dp)
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .background(panelBackground),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailButton(
                tooltip = "Projects",
                selected = modpack.selectedTab == SelectedTab.PROJECTS,
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.PROJECTS)) },
            ) {
                Icon(
                    key = PakkuDesktopIcons.Modpack.manage,
                    contentDescription = "projects icon",
                    tint = it,
                    hints = arrayOf()
                )
            }
            RailButton(
                tooltip = "Modpack",
                selected = modpack.selectedTab == SelectedTab.MODPACK,
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.MODPACK)) },
            ) {
                Icon(
                    key = PakkuDesktopIcons.properties,
                    contentDescription = "modpack icon",
                    tint = it,
                    hints = arrayOf()
                )
            }
            Pro(appModel) {
                RailButton(
                    tooltip = "Source control",
                    selected = modpack.selectedTab == SelectedTab.COMMIT,
                    onClick = { publish(ModpackMsg.TabSelected(SelectedTab.COMMIT)) },
                ) {
                    Icon(
                        key = AllIconsKeys.Toolwindows.ToolWindowCommit,
                        contentDescription = "commit icon",
                        tint = it,
                        hints = arrayOf()
                    )
                }
            }
        }
        Column {
            Spacer(Modifier.background(borderColor).width(1.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun RailButton(
    tooltip: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
)
{
    val tint = if (selected) PakkuDesktopConstants.highlightColor else Color.Gray
    PkUiTooltip({ Text(tooltip) }) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (selected) PakkuDesktopConstants.highlightColor.copy(alpha = 0.14f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onClick,
                Modifier.size(36.dp),
                enabled = !selected,
            ) {
                icon(tint)
            }
        }
    }
}
