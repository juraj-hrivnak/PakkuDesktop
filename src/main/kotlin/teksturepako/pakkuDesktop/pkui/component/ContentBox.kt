/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    shape: Shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
    content: @Composable BoxScope.() -> Unit,
)
{
    val background = animatedColor(JewelTheme.globalColors.panelBackground)
    val border = animatedColor(color)

    Box(
        modifier
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = shape,
                ambientColor = border,
                clip = false,
            )
            .background(
                color = background,
                shape = shape,
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = border,
                shape = shape,
            )
            .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
    ) {
        content(this)
    }
}
