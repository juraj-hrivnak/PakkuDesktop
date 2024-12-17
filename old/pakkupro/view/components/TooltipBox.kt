package pakkupro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.foundation.theme.OverrideDarkMode
import org.jetbrains.jewel.ui.theme.tooltipStyle
import org.jetbrains.jewel.ui.util.isDark

@Composable
fun TooltipBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
{
    CompositionLocalProvider(
        LocalContentColor provides JewelTheme.tooltipStyle.colors.content,
    ) {
        Box(
            modifier
                .shadow(
                    elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                    shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                    ambientColor = JewelTheme.tooltipStyle.colors.shadow,
                    spotColor = Color.Transparent,
                )
                .background(
                    color = JewelTheme.tooltipStyle.colors.background,
                    shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                )
                .border(
                    width = JewelTheme.tooltipStyle.metrics.borderWidth,
                    color = JewelTheme.tooltipStyle.colors.border,
                    shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                )
                .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
        ) {
            OverrideDarkMode(JewelTheme.tooltipStyle.colors.background.isDark()) {
                content()
            }
        }
    }
}
