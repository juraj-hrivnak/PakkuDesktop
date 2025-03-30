package teksturepako.pakkupro.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.component.ContentBox
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloseDialog()
{
    val profileData by ProfileViewModel.profileData.collectAsState()
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (profileData.closeDialog != null)
    {
        Dialog(onDismissRequest = { ProfileViewModel.dismissCloseDialog() }) {
            ContentBox {
                Row(
                    Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        modpackUiState.action.first
                            ?.let {
                                Header(
                                    text = "Action '$it' is running.",
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "Do you want to terminate the action?",
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            ?: Header(
                                text = "Do you want to close this modpack?",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (profileData.closeDialog!!.forceClose)
                                    {
                                        // Close and then terminate action
                                        coroutineScope.launch {
                                            profileData.closeDialog!!.onClose.invoke()
                                            ModpackViewModel.terminateAction()
                                        }
                                    }
                                    else
                                    {
                                        // Close and terminate action simultaneously
                                        coroutineScope.launch {
                                            ModpackViewModel.terminateAction()
                                        }
                                        coroutineScope.launch {
                                            profileData.closeDialog!!.onClose.invoke()
                                        }
                                    }
                                    ProfileViewModel.dismissCloseDialog()
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("Yes")
                            }
                            DefaultButton(
                                onClick = { ProfileViewModel.dismissCloseDialog() },
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("No")
                            }
                        }
                    }
                }
            }
        }
    }
}
