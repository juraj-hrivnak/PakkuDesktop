package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    shape: Shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
    content: @Composable BoxScope.() -> Unit,
)
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    val themeDefinition = if (profileData.intUiTheme.isDark())
    {
        JewelTheme.darkThemeDefinition()
    }
    else
    {
        JewelTheme.lightThemeDefinition()
    }

    val background = remember { Animatable(themeDefinition.globalColors.panelBackground) }

    LaunchedEffect(profileData.intUiTheme) {
        background.animateTo(themeDefinition.globalColors.panelBackground, animationSpec = tween(200))
    }

    Box(
        modifier
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = shape,
                ambientColor = color,
                spotColor = Color.Transparent,
            )
            .background(
                color = background.value,
                shape = shape,
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = color,
                shape = shape,
            )
            .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
    ) {
        content(this)
    }
}
