/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import io.github.kdroidfilter.nucleus.window.TitleBarScope
import io.github.kdroidfilter.nucleus.window.jewel.JewelTitleBar
import io.github.kdroidfilter.nucleus.window.newFullscreenControls
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun PakkuApplicationScope.MainTitleBar(
    modifier: Modifier,
    withGradient: Boolean = false,
    themeTrailingActions: @Composable RowScope.() -> Unit = { },
    content: @Composable TitleBarScope.() -> Unit = { },
)
{
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)
    Column {
        Row {
        this@MainTitleBar.decoratedWindowScope.JewelTitleBar(
                modifier.newFullscreenControls(),
                gradientStartColor = if (withGradient) Color(16, 77, 69) else Color.Unspecified
            ) {
                AlignedTitleBarContent(modifier.padding(horizontal = 8.dp), alignment = Alignment.Start) {
                    Icon(
                        PakkuDesktopIcons.pakku,
                        "pakku",
                        Modifier.size(25.dp)
                    )
                }

                AlignedTitleBarContent(modifier.padding(horizontal = 8.dp), alignment = Alignment.End) {
                    themeTrailingActions()
                    AlphaLabel()
                }

                content(this@JewelTitleBar)
            }
        }
        Row {
            Spacer(Modifier.background(borderColor).height(1.dp).fillMaxWidth())
        }
    }
}

@Composable
fun TitleBarScope.AlignedTitleBarContent(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal,
    content: @Composable RowScope.() -> Unit
)
{
    Row(
        modifier.align(alignment),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(this)
    }
}
