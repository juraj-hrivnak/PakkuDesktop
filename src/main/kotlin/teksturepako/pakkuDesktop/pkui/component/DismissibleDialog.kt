package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun DismissibleDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    maxWidth: Dp = 1200.dp,
    maxHeight: Dp = 2000.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (visible)
    {
        Dialog(
            properties = DialogProperties(
                dismissOnClickOutside = true
            ),
            onDismissRequest = onDismiss
        ) {
            ContentBox(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
            ) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    // Close button in the top-right corner
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(
                            AllIconsKeys.General.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Main content with padding to accommodate close button
                Box(
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .widthIn(max = maxWidth - 32.dp) // Account for padding
                        .heightIn(max = maxHeight - 48.dp) // Account for padding and close button
                ) {
                    content(this)
                }
            }
        }
    }
}