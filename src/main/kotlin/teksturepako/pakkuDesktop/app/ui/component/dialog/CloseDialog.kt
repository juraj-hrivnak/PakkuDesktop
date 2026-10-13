/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.CloseDialogRequest
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

/**
 * A declarative dialog — shown when [model.closeDialog] is non-null.
 * No driver needed; pure composable shown conditionally from view.
 * Enter confirms; Esc dismisses.
 */
@Composable
fun CloseDialog(publish: (AppMsg) -> Unit, model: AppModel) {
    val request = model.closeDialog ?: return
    val actionName = model.modpack.actionName
    val quitting = request is CloseDialogRequest.Quit
    val onDismiss = { publish(AppMsg.DismissCloseDialog) }
    val onConfirm = { publish(AppMsg.ConfirmCloseDialog) }

    val bodyMessage = when {
        actionName != null -> "Do you want to terminate the action?"
        request is CloseDialogRequest.OpenDirectory -> "'${request.path}' will be opened instead."
        else -> null
    }

    PkUiDialog(
        onDismiss = onDismiss,
        title = when {
            actionName != null -> "Action '$actionName' is running."
            else -> "Do you want to close this modpack?"
        },
        icon = if (actionName != null) AllIconsKeys.General.WarningDialog else AllIconsKeys.General.QuestionDialog,
        size = PkUiDialogSize.Small,
        scrollable = bodyMessage != null,
        onConfirm = onConfirm,
        focusKey = request,
        footer = {
            PkUiDialogButtons(
                primaryLabel = when {
                    actionName != null -> "Terminate"
                    quitting -> "Quit"
                    else -> "Close"
                },
                onPrimary = onConfirm,
                onSecondary = onDismiss,
            )
        },
    ) {
        if (bodyMessage != null) {
            Text(
                bodyMessage,
                color = JewelTheme.contentColor.copy(alpha = 0.75f),
            )
        }
    }
}
