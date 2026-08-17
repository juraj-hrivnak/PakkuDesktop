/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.TextArea
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

@Composable
fun ErrorDialog(error: ActionError, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(error) {
        runCatching { focusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        ContentBox(
            Modifier
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(onDismiss = onDismiss)
                .widthIn(max = 640.dp),
        ) {
            Column(Modifier.padding(PakkuDesktopConstants.commonPaddingSize)) {
                error::class.simpleName?.let {
                    Header("Error of type '$it' occurred.", Modifier.padding(vertical = 4.dp))
                }
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CopyToClipboardButton(
                        text = error.rawMessage,
                        buttonSize = 35.dp,
                        iconSize = 18.dp,
                        useSimpleTooltip = true,
                    )
                }
                TextArea(
                    TextFieldState(error.rawMessage),
                    readOnly = true,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textStyle = JewelTheme.consoleTextStyle,
                )
            }
        }
    }
}
