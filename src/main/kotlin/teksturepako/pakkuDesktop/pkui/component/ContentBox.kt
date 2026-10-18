/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    shape: Shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
    contentPadding: PaddingValues = JewelTheme.tooltipStyle.metrics.contentPadding,
    elevation: Dp = JewelTheme.tooltipStyle.metrics.shadowSize,
    ambientShadowColor: Color? = null,
    spotShadowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
)
{
    val background = animatedColor(JewelTheme.globalColors.panelBackground)
    val border = animatedColor(color)
    val customShadow = ambientShadowColor ?: spotShadowColor
    val shadowModifier = if (customShadow != null) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambientShadowColor ?: customShadow,
            spotColor = spotShadowColor ?: customShadow,
        )
    } else {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = border,
            clip = false,
        )
    }

    Box(
        modifier.then(shadowModifier)
            .background(
                color = background,
                shape = shape,
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = border,
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        content(this)
    }
}
