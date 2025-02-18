package teksturepako.pakkupro.ui.component.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyToClipboardButton(
    text: String,
    modifier: Modifier = Modifier,
    useSimpleTooltip: Boolean = false
)
{
    val clipboardManager = LocalClipboardManager.current
    var tooltipExpanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            tooltipExpanded = true
            clipboardManager.setText(AnnotatedString((text)))
        },
        modifier
    ) {
        Tooltip({
            if (useSimpleTooltip)
            {
                Text("Copy to clipboard")
            }
            else
            {
                Text("Copy \"$text\" to clipboard")
            }
        }) {
            Icon(
                key = PakkuDesktopIcons.clone,
                contentDescription = "copy",
                tint = Color.Gray,
                hints = arrayOf(),
            )
        }
    }

    if (tooltipExpanded)
    {
        PopupMenu(
            onDismissRequest = {
                tooltipExpanded = false
                true
            },
            horizontalAlignment = Alignment.Start,
            popupProperties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = false,
            )
        ) {
            passiveItem {
                Text(
                    if (useSimpleTooltip) "copied to clipboard" else "\"$text\" copied to clipboard",
                    Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit)
    {
        delay(5.seconds)
        tooltipExpanded = false
    }
}