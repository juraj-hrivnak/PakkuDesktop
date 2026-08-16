/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.text.Header

/**
 * In-window modal dialog. Uses [Popup] (not AWT [androidx.compose.ui.window.Dialog])
 * so it works on the Nucleus Tao backend.
 *
 * **Esc** dismisses; **Enter** invokes [onConfirm] when provided.
 */
@Composable
fun PkUiDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    onConfirm: (() -> Unit)? = null,
    maxWidth: Dp = 1200.dp,
    maxHeight: Dp = 2000.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (!visible) return

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(visible) {
        if (visible) runCatching { focusRequester.requestFocus() }
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(onDismiss = onDismiss, onConfirm = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            ContentBox(
                Modifier
                    .padding(16.dp)
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
            ) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    title?.let {
                        Header(
                            text = title,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            AllIconsKeys.General.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Box(
                    Modifier
                        .padding(top = 32.dp)
                        .widthIn(max = maxWidth - 32.dp)
                        .heightIn(max = maxHeight - 48.dp)
                ) {
                    content(this)
                }
            }
        }
    }
}
