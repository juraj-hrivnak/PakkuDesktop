package teksturepako.pakkupro.ui.component.dialog.git

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
import teksturepako.pakkupro.ui.component.dialog.DismissibleDialog
import teksturepako.pakkupro.ui.viewmodel.GitViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PushDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
)
{
    val gitState by GitViewModel.gitState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    DismissibleDialog(visible, onDismiss) {
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