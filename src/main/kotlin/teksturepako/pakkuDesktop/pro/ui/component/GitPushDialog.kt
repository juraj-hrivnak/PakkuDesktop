/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pro.ui.viewmodel.GitViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitPushDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
)
{
    val gitState by GitViewModel.gitState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    PkUiDialog(visible, onDismiss) {
        FlowColumn(
            Modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            gitState.outgoingCommits.forEach { outgoingCommit ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(outgoingCommit.hash)
                    Text(outgoingCommit.message)
                }
            }
            Row {
                DefaultButton(
                    onClick = {
                        coroutineScope.launch {
                            GitViewModel.push()
                        }
                    }) {
                    Text("Push")
                }
            }
        }
    }
}