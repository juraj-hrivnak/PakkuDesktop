package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun DismissiblePopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    maxWidth: Dp = 600.dp,
    maxHeight: Dp = 400.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (visible)
    {
        var position by remember { mutableStateOf(IntOffset.Zero) }
        var initiallyPositioned by remember { mutableStateOf(false) }
        var popupSize by remember { mutableStateOf(IntSize.Zero) }

        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    if (!initiallyPositioned)
                    {
                        // Initial center position
                        position = IntOffset(
                            x = (windowSize.width - popupContentSize.width) / 2,
                            y = (windowSize.height - popupContentSize.height) / 2
                        )
                        initiallyPositioned = true
                    }

                    // Keep popup within window bounds
                    val x = position.x.coerceIn(0, windowSize.width - popupContentSize.width)
                    val y = position.y.coerceIn(0, windowSize.height - popupContentSize.height)

                    return IntOffset(x, y)
                }
            },
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = false
            ),
            onDismissRequest = onDismiss
        ) {
            ContentBox(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
                    .onGloballyPositioned { coordinates ->
                        popupSize = coordinates.size
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            position = position.plus(
                                IntOffset(
                                    x = dragAmount.x.toInt(),
                                    y = dragAmount.y.toInt()
                                )
                            )
                        }
                    }
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