/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

/** Fixed hit target; [buttonSize]/[iconSize] are Dp so they scale with density. */
@Composable
fun CopyToClipboardButton(
    text: String,
    modifier: Modifier = Modifier,
    useSimpleTooltip: Boolean = false,
    buttonSize: Dp = 24.dp,
    iconSize: Dp = 14.dp,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    PkUiTooltip(
        tooltip = {
            if (useSimpleTooltip) {
                Text("Copy to clipboard")
            } else {
                Text("Copy \"$text\" to clipboard")
            }
        },
    ) {
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(text))
            },
            modifier = modifier.size(buttonSize),
        ) {
            Icon(
                key = PakkuDesktopIcons.clone,
                contentDescription = "copy",
                tint = Color.Gray,
                hints = arrayOf(),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
