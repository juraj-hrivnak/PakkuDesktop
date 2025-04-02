package teksturepako.pakkupro.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkupro.ui.component.dialog.git.PushDialog
import teksturepako.pakkupro.ui.viewmodel.GitViewModel
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab

@Composable
fun VcsDropdown()
{
    val profileData by ProfileViewModel.profileData.collectAsState()
    val gitState by GitViewModel.gitState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // -- DIALOGS --

    var pushDialogVisible by remember { mutableStateOf(false) }

    PushDialog(
        pushDialogVisible,
        onDismiss = { pushDialogVisible = false }
    )

    // -- DROPDOWN --

    DropdownImpl(
        Modifier.padding(vertical = 4.dp),
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AllIconsKeys.General.Vcs, "vcs")
                Text(gitState.branches.firstOrNull { it.isCurrent }?.name ?: return@DropdownImpl)
            }
        },
        menuModifier = Modifier
            .offset(x = 12.dp)
            .width(200.dp),
        menuContent = {
            selectableItem(false, onClick = {
                coroutineScope.launch {
                    GitViewModel.pull()
                }
            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Actions.CheckOut,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Pull...",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            selectableItem(false, onClick = {
                ModpackViewModel.selectTab(SelectedTab.COMMIT)
            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Actions.Commit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Commit...",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            selectableItem(false, onClick = {
                coroutineScope.launch {
                    GitViewModel.push()
                }
            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Vcs.Push,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        val outgoingCommits = gitState.outgoingCommits.size
                        Text(
                            "Push... $outgoingCommits",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            separator()

            passiveItem {
                Row(
                    Modifier.padding(start = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        "Local Branches",
                        color = Color.Gray,
                    )
                }
            }

            gitState.branches.filterNot { it.isRemote }.forEach { branch ->
                selectableItem(false, onClick = {
                    coroutineScope.launch {
                        GitViewModel.checkout(branch)
                    }
                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {}
                        Column {
                            Text(
                                branch.name,
                                Modifier,
                                color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            passiveItem {
                Row(
                    Modifier.padding(start = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        "Remote Branches",
                        color = Color.Gray,
                    )
                }
            }

            gitState.branches.filter { it.isRemote }.forEach { branch ->
                selectableItem(false, onClick = {
                    coroutineScope.launch {
                        GitViewModel.checkout(branch)
                    }
                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {}
                        Column {
                            Text(
                                branch.name,
                                Modifier,
                                color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    )

    val gitEventProgress by GitViewModel.eventProgress.collectAsState()

    gitEventProgress?.let { event ->
        Text(event.operation)
        HorizontalProgressBar(event.percentage, Modifier.width(60.dp))
    }
}
