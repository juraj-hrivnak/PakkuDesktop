/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog

@Composable
fun AddProjectsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    model: AppModel,
) {
    val lockFile = model.modpack.lockFile?.get() ?: return

    val projectProvider = lockFile.getProjectProvider().getOrElse { return }

    val textFieldState = rememberTextFieldState()

    PkUiDialog(visible = visible, onDismiss = onDismiss) {
        FlowColumn(Modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Header("Add Projects")
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Enter the projects to add")
            Spacer(Modifier.height(8.dp))
            TextField(state = textFieldState)
        }
    }
}