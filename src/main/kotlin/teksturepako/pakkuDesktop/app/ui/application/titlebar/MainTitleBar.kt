/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import dev.nucleusframework.window.jewel.JewelTitleBar
import dev.nucleusframework.window.newFullscreenControls
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun PakkuApplicationScope.MainTitleBar(
    modifier: Modifier,
    themeTrailingActions: @Composable RowScope.() -> Unit = { },
    startContent: @Composable RowScope.() -> Unit = { },
    endContent: @Composable RowScope.() -> Unit = { },
) {
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)
    val layout = rememberTitleBarLayout()
    Column {
        Row {
            CompositionLocalProvider(
                LocalTitleBarWidthTier provides layout.tier,
                LocalTitleBarStartSlotMaxWidth provides layout.startSlotMaxWidth,
                LocalTitleBarStartClusterMaxWidth provides layout.startClusterMaxWidth,
            ) {
                this@MainTitleBar.decoratedWindowScope.JewelTitleBar(
                    modifier.newFullscreenControls(),
                ) {
                    AlignedTitleBarContent(modifier.padding(horizontal = 8.dp), alignment = Alignment.End) {
                        themeTrailingActions()
                        if (layout.tier == TitleBarWidthTier.Comfortable) {
                            AlphaLabel()
                        }
                    }

                    AlignedTitleBarContent(alignment = Alignment.Start) {
                        TitleBarStartCluster(startContent = startContent)
                    }

                    AlignedTitleBarContent(alignment = Alignment.End) {
                        endContent()
                    }
                }
            }
        }
        Row {
            Spacer(Modifier.background(borderColor).height(1.dp).fillMaxWidth())
        }
    }
}

/** Brand icon and view menus share one capped start cluster so Nucleus never drops the icon. */
@Composable
private fun TitleBarStartCluster(startContent: @Composable RowScope.() -> Unit) {
    val clusterMax = LocalTitleBarStartClusterMaxWidth.current
    Row(
        Modifier
            .then(
                if (clusterMax.isUnspecified) Modifier else Modifier.widthIn(max = clusterMax),
            )
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.width(PakkuDesktopConstants.railWidth),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PakkuDesktopIcons.pakku,
                "pakku",
                Modifier.size(PakkuDesktopConstants.brandIconSize),
            )
        }
        Row(
            Modifier
                .weight(1f, fill = true)
                .widthIn(min = 0.dp)
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            startContent()
        }
    }
}

/** Menus and labels inside [TitleBarStartCluster]; truncates within the remaining width after the brand icon. */
@Composable
fun TitleBarStartRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
fun dev.nucleusframework.window.TitleBarScope.AlignedTitleBarContent(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.align(alignment),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(this)
    }
}

private val Dp.isUnspecified: Boolean
    get() = this == Dp.Unspecified
