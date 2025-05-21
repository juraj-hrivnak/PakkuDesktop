package teksturepako.pakkuDesktop.ui.component.dialog

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
import teksturepako.pakkuDesktop.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.ui.component.ContentBox
import teksturepako.pakkuDesktop.ui.component.text.Header
import teksturepako.pakkuDesktop.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateModpackDialog()
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { ProfileViewModel.dismissCloseDialog() },
    ) {
        ContentBox {
            Row(
                Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Header(
                        text = "Modpack '${profileData.currentProfile?.name}' is not not initialized.",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Do you want to create a new modpack?",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    FlowRow(
                        verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = {

                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Yes")
                        }
                        DefaultButton(
                            onClick = {
                                coroutineScope.launch {
                                    ProfileViewModel.updateCurrentProfile(null)
                                }
                            },
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