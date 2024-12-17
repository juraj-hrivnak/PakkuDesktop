package pakkupro.view.components

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
import pakkupro.viewmodel.PakkuDesktopIcons

@Composable
fun ContentBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
{
    Box(
        modifier
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                ambientColor = JewelTheme.globalColors.borders.disabled,
                spotColor = Color.Transparent,
            )
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
            )
            .border(
                width = JewelTheme.tooltipStyle.metrics.borderWidth,
                color = JewelTheme.globalColors.borders.disabled,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
            )
            .padding(JewelTheme.tooltipStyle.metrics.contentPadding),
    ) {
        content()
    }
}

@Composable
fun ContentBoxTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    onEnterPressed: (() -> Unit)? = null,
    onRemoveClicked: (() -> Unit)? = null,
    removeButtonEnabled: Boolean = true,
) {
    ContentBox {
        Row(horizontalArrangement = Arrangement.Center) {
            Column {
                TextField(
                    state,
                    modifier.onKeyEvent { keyEvent ->
                        if (keyEvent.key != Key.Enter) return@onKeyEvent false
                        if (keyEvent.type == KeyEventType.KeyUp)
                        {
                            if (onEnterPressed == null) return@onKeyEvent false else onEnterPressed()
                        }

                        true
                    },
                )
            }
            if (state.text.isNotBlank() && removeButtonEnabled)
            {
                Column(modifier.padding(start = 1.dp)) {
                    IconButton(
                        onClick = {
                            if (onRemoveClicked != null)
                            {
                                onRemoveClicked()
                            }
                        }
                    ) {
                        Icon(PakkuDesktopIcons.remove, "remove", Modifier.size(30.dp), tint = Color.Gray)
                    }
                }
            }
        }
    }
}
