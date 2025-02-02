package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.tooltipStyle

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    shape: Shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
    content: @Composable () -> Unit,
)
{
    Box(
        modifier
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = shape,
                ambientColor = color,
                spotColor = Color.Transparent,
            )
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = shape,
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = color,
                shape = shape,
            )
            .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
    ) {
        content()
    }
}
