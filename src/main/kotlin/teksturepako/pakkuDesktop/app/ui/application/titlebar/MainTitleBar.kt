package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.TitleBarScope
import org.jetbrains.jewel.window.newFullscreenControls
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton

@Composable
fun PakkuApplicationScope.MainTitleBar(
    modifier: Modifier,
    withGradient: Boolean = false,
    content: @Composable TitleBarScope.() -> Unit = { }
)
{
    Column {
        Row {
            this@MainTitleBar.decoratedWindowScope.TitleBar(
                modifier.newFullscreenControls(),
                gradientStartColor = if (withGradient) Color(16, 77, 69) else Color.Unspecified
            ) {
                AlignedTitleBarContent(modifier, alignment = Alignment.Start) {
                    Icon(
                        PakkuDesktopIcons.pakku,
                        "pakku",
                        Modifier.size(25.dp)
                    )

                    content(this@TitleBar)
                }
                AlignedTitleBarContent(modifier, alignment = Alignment.End) {
                    ThemeButton()
                }
            }
        }
        Row {
            Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())
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
        modifier
            .align(alignment)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(this)
    }
}
