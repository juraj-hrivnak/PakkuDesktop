/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog

@Composable
fun GitPushDialog(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val gitState = model.git

    PkUiDialog(visible, onDismiss) {
        FlowColumn(
            Modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            gitState.outgoingCommits.forEach { outgoingCommit ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(outgoingCommit.hash)
                    Text(outgoingCommit.message)
                }
            }
            Row {
                DefaultButton(
                    onClick = { publish(ModpackMsg.GitPushRequested) },
                ) {
                    Text("Push")
                }
            }
        }
    }
}
