/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogWindow

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    PkUiDialogWindow(
        visible = true,
        onDismiss = onDismiss,
        title = "Settings"
    ) {
        // Settings content goes here
    }
}
