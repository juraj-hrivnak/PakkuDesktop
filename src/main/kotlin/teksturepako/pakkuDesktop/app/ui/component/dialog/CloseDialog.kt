/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

/**
 * A declarative dialog — shown when [model.closeDialog] is non-null.
 * No driver needed; pure composable shown conditionally from view.
 * Enter confirms; Esc dismisses.
 */
@Composable
fun CloseDialog(publish: (AppMsg) -> Unit, model: AppModel) {
    val request = model.closeDialog ?: return
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(request) {
        runCatching { focusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = { publish(AppMsg.DismissCloseDialog) }) {
        ContentBox(
            Modifier
                .focusRequester(focusRequester)
                .dialogConfirmCancelKeys(
                    onDismiss = { publish(AppMsg.DismissCloseDialog) },
                    onConfirm = { publish(AppMsg.ConfirmCloseDialog) },
                )
        ) {
            Row(
                Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    model.modpack.actionName
                        ?.let {
                            Header("Action '$it' is running.", Modifier.padding(vertical = 4.dp))
                            Text("Do you want to terminate the action?", Modifier.padding(vertical = 4.dp))
                        }
                        ?: Header("Do you want to close this modpack?", Modifier.padding(vertical = 4.dp))

                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = { publish(AppMsg.ConfirmCloseDialog) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) { Text("Yes") }

                        DefaultButton(
                            onClick = { publish(AppMsg.DismissCloseDialog) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) { Text("No") }
                    }
                }
            }
        }
    }
}
