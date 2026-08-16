/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Enter → [onConfirm] (when non-null); Esc → [onDismiss].
 * Returns a [Modifier] suitable for dialog / popup roots.
 */
fun Modifier.dialogConfirmCancelKeys(
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.Escape -> {
            onDismiss()
            true
        }
        Key.Enter, Key.NumPadEnter -> {
            if (onConfirm != null) {
                onConfirm()
                true
            } else false
        }
        else -> false
    }
}
