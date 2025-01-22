package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkupro.ui.PakkuDesktopIcons

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    content: @Composable () -> Unit,
)
{
    Box(
        modifier
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                ambientColor = color,
                spotColor = Color.Transparent,
            )
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = color,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
            )
            .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
    ) {
        content()
    }
}
