/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.integration.writeClipboardText
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
    val scope = rememberCoroutineScope()

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
                val payload = text
                scope.launch(Dispatchers.IO) {
                    writeClipboardText(payload)
                }
            },
            modifier = modifier.size(buttonSize),
        ) {
            Icon(
                key = AllIconsKeys.General.Copy,
                contentDescription = "copy",
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
