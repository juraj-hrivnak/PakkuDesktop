/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Enter → [onConfirm] (when non-null); Esc → [onDismiss].
 * With [confirmNeedsAccelerator], only Ctrl/Cmd+Enter confirms so plain Enter reaches a
 * multi-line text field.
 * Returns a [Modifier] suitable for dialog / popup roots.
 */
fun Modifier.dialogConfirmCancelKeys(
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmNeedsAccelerator: Boolean = false,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.Escape -> {
            onDismiss()
            true
        }
        Key.Enter, Key.NumPadEnter -> {
            val accelerator = event.isCtrlPressed || event.isMetaPressed
            if (onConfirm != null && (accelerator || !confirmNeedsAccelerator)) {
                onConfirm()
                true
            } else false
        }
        else -> false
    }
}
