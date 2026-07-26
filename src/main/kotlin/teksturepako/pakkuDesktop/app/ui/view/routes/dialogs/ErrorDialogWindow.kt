/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.getError
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.TextArea
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog

@Composable
fun ErrorDialog(model: AppModel, onDismiss: () -> Unit) {
    val error = model.modpack.lockFile?.getError() ?: run {
        onDismiss()
        return
    }

    PkUiDialog(
        visible = true,
        onDismiss = onDismiss,
        title = error::class.simpleName?.let { "Error of type '$it' occurred." },
    ) {
        Row(
            Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CopyToClipboardButton(error.rawMessage, Modifier.size(35.dp), useSimpleTooltip = true)
                }
                TextArea(
                    TextFieldState(error.rawMessage),
                    readOnly = true,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textStyle = JewelTheme.consoleTextStyle
                )
            }
        }
    }
}
