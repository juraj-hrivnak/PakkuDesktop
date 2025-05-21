package teksturepako.pakkuDesktop.ui.component.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.ui.component.ImmediateTooltip
import teksturepako.pakkuDesktop.ui.component.TooltipPosition

@Composable
fun CopyToClipboardButton(
    text: String,
    modifier: Modifier = Modifier,
    useSimpleTooltip: Boolean = false
)
{
    val clipboardManager = LocalClipboardManager.current

    ImmediateTooltip(
        tooltip = {
            if (useSimpleTooltip)
            {
                Text("Copy to clipboard")
            }
            else
            {
                Text("Copy \"$text\" to clipboard")
            }
        },
        position = TooltipPosition.END
    ) {
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString((text)))
            },
            modifier
        ) {
            Icon(
                key = PakkuDesktopIcons.clone,
                contentDescription = "copy",
                tint = Color.Gray,
                hints = arrayOf(),
            )
        }
    }
}