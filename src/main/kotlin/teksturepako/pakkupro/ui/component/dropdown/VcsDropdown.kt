package teksturepako.pakkupro.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.fold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkupro.actions.Git
import teksturepako.pakkupro.actions.GitError
import teksturepako.pakkupro.actions.GitEvent
import teksturepako.pakkupro.actions.exec
import teksturepako.pakkupro.ui.component.showToast
import teksturepako.pakkupro.ui.viewmodel.GitViewModel
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab
import kotlin.io.path.Path

@Composable
fun VcsDropdown()
{
    val profileData by ProfileViewModel.profileData.collectAsState()
    val gitState by GitViewModel.state.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit)
    {
        GitViewModel.initialize(Path(workingPath))
    }

    val branch = gitState.repository?.branch ?: return

    DropdownImpl(
        Modifier.padding(vertical = 4.dp),
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AllIconsKeys.General.Vcs, "vcs")
                Text(branch)
            }
        },
        menuModifier = Modifier
            .offset(x = 12.dp)
            .width(160.dp),
        menuContent = {
            selectableItem(false, onClick = {

            }) {
                Row {
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
                Row {
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
                val git = Git.at(workingPath)

                coroutineScope.launch {
                    (git exec "push --progress origin HEAD")
                        .collect { result ->
                            result.fold(
                                success = { event ->
                                    when (event)
                                    {
                                        is GitEvent.Progress -> println("Progress: ${event.percentage}")
                                        is GitEvent.Output   ->
                                        {
                                            withContext(Dispatchers.Main) {
                                                ModpackViewModel.toasts.showToast {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(16.dp)
                                                            .width(300.dp)
                                                    ) {
                                                        Text(event.message)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                failure = { error ->
                                    when (error)
                                    {
                                        is GitError.Command ->
                                        {
                                            withContext(Dispatchers.Main) {
                                                ModpackViewModel.toasts.showToast {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(16.dp)
                                                            .width(300.dp)
                                                    ) {
                                                        Text(error.message)
                                                    }
                                                }
                                            }
                                        }
                                        else -> { }
                                    }
                                },
                            )
                        }
                }

            }) {
                Row {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = AllIconsKeys.Vcs.Push,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Push...",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    )
}
