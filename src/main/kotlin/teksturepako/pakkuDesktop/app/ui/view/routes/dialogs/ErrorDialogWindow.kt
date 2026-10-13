/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

@Composable
fun ErrorDialog(error: ActionError, onDismiss: () -> Unit) {
    val details = remember(error) { TextFieldState(error.rawMessage) }

    PkUiDialog(
        onDismiss = onDismiss,
        title = "Something went wrong",
        icon = AllIconsKeys.General.ErrorDialog,
        subtitle = error::class.simpleName,
        size = PkUiDialogSize.Large,
        onConfirm = onDismiss,
        focusKey = error,
        footerStart = {
            CopyToClipboardButton(
                text = error.rawMessage,
                buttonSize = 30.dp,
                iconSize = 16.dp,
                useSimpleTooltip = true,
            )
        },
        footer = {
            DefaultButton(onClick = onDismiss) { Text("OK") }
        },
    ) {
        ActionErrorContent(error)
        TextArea(
            details,
            readOnly = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 320.dp),
            textStyle = JewelTheme.consoleTextStyle,
        )
    }
}
