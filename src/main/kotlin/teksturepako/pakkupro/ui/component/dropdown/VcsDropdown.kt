package teksturepako.pakkupro.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.data.workingPath
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
