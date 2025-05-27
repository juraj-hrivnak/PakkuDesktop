/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.text.Header

@Composable
fun PkUiDialogWindow(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val dialogState = rememberDialogState()

    DialogWindow(
        visible = visible,
        onCloseRequest = { onDismiss() },
        state = dialogState,
        undecorated = true,
        resizable = false
    ) {
        WindowDraggableArea {
            ContentBox(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Title on the left
                    title?.let {
                        Header(
                            text = title,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }

                    // Close button stays on the right
                    IconButton(
                        onClick = onDismiss, modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            AllIconsKeys.General.Close, contentDescription = "Close"
                        )
                    }
                }

                Box(
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    content(this)
                }
            }
        }
    }
}