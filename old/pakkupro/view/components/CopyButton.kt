package pakkupro.view.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.*
import pakkupro.viewmodel.PakkuDesktopIcons
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyButton(
    onClick: (ClipboardManager) -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String = ""
)
{
    val clipboardManager = LocalClipboardManager.current
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            expanded = true
            onClick(clipboardManager)
        },
        modifier
    ) {
        Tooltip({ Text("Copy $tooltip") }) {
            Icon(
                key = PakkuDesktopIcons.clone,
                contentDescription = "copy",
                tint = Color.Gray,
                hints = arrayOf(),
            )
        }
    }

    if (expanded)
    {
        PopupMenu(
            onDismissRequest = {
                expanded = false
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
                    "\"${clipboardManager.getText()}\" copied to clipboard",
                    Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit)
    {
        delay(5.seconds)
        expanded = false
    }
}