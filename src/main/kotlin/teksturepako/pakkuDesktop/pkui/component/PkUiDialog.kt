/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.text.Header

@Composable
fun PkUiDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
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
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            AllIconsKeys.General.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Main content with padding to accommodate header row
                Box(
                    modifier = Modifier
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