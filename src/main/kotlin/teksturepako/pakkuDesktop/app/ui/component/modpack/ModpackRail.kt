/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.IconKey
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

private val RailButtonSize = 36.dp
private val RailIconSize = 24.dp
private val RailButtonShape = RoundedCornerShape(6.dp)

/** Left navigation rail — width matches the title-bar Pakku icon slot. */
@Composable
fun ModpackRail(
    publish: (ModpackMsg) -> Unit,
    modpack: ModpackModel,
    appModel: AppModel,
) {
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)

    Row {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(PakkuDesktopConstants.railWidth)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RailButton(
                key = PakkuDesktopIcons.Modpack.manage,
                tooltip = "Projects",
                selected = modpack.selectedTab == SelectedTab.PROJECTS,
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.PROJECTS)) },
            )
            RailButton(
                key = PakkuDesktopIcons.properties,
                tooltip = "Modpack",
                selected = modpack.selectedTab == SelectedTab.MODPACK,
                onClick = { publish(ModpackMsg.TabSelected(SelectedTab.MODPACK)) },
            )
            Pro(appModel) {
                RailButton(
                    key = AllIconsKeys.Toolwindows.ToolWindowCommit,
                    tooltip = "Source control",
                    selected = modpack.selectedTab == SelectedTab.COMMIT,
                    onClick = { publish(ModpackMsg.TabSelected(SelectedTab.COMMIT)) },
                )
            }
        }
        Spacer(Modifier.background(borderColor).width(1.dp).fillMaxHeight())
    }
}

@Composable
private fun RailButton(
    key: IconKey,
    tooltip: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val content = JewelTheme.contentColor

    // Theme content color only — avoid SelectableIconButton’s opaque selected fill (reads as black).
    val tint = when {
        selected -> content
        hovered -> content.copy(alpha = 0.9f)
        else -> content.copy(alpha = 0.7f)
    }
    val fill = when {
        selected -> content.copy(alpha = 0.12f)
        hovered -> content.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    PkUiTooltip({ Text(tooltip) }) {
        Box(
            modifier = Modifier
                .size(RailButtonSize)
                .clip(RailButtonShape)
                .background(fill, RailButtonShape)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !selected,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                key = key,
                contentDescription = tooltip,
                tint = tint,
                hints = arrayOf(),
                modifier = Modifier.size(RailIconSize),
            )
        }
    }
}
