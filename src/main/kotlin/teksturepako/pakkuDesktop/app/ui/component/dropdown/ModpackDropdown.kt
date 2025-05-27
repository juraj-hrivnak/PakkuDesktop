package teksturepako.pakkuDesktop.app.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import teksturepako.pakkuDesktop.app.actions.exportImpl
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown
import kotlin.io.path.Path

@Composable
fun ModpackDropdown(
    pickerLauncher: PickerResultLauncher,
    navController: NavHostController,
    enabled: Boolean = true,
)
{
    val profileData by ProfileViewModel.profileData.collectAsState()
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    PkUiDropdown(
        Modifier.padding(vertical = 4.dp),
        enabled = enabled,
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                profileData.currentProfile?.name?.let { Text(it) } ?: Text("Modpack")
            }
        },
        menuModifier = Modifier
            .width(200.dp),
        menuContent = {

            // -- OPEN --

            selectableItem(false, onClick = {
                pickerLauncher.launch()
            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.open,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Open...",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            // -- CLOSE --

            selectableItem(false, onClick = {
                if (modpackUiState.action.first != null)
                {
                    ProfileViewModel.updateCloseDialog {
                        ProfileViewModel.updateCurrentProfile(null)
                        navController.popBackStack()
                    }
                }
                else
                {
                    coroutineScope.launch {
                        ProfileViewModel.updateCurrentProfile(null)
                        navController.popBackStack()
                    }
                }
            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                    }
                    Column {
                        Text(
                            "Close",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            separator()

            // -- EXPORT --

            selectableItem(
                selected = false,
                onClick = {
                    exportImpl(modpackUiState)
                },
                enabled = modpackUiState.action.first == null
            ) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.cube,
                            "export",
                            modifier = Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Export",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            separator()

            // -- FETCH --

            selectableItem(false, onClick = {

            }) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.2f)) {
                        Icon(
                            key = PakkuDesktopIcons.cloudDownload,
                            contentDescription = "fetch",
                            Modifier.size(15.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    Column {
                        Text(
                            "Fetch",
                            Modifier,
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }
            }

            if (profileData.recentProfilesFiltered.isNotEmpty())
            {
                separator()

                // -- RECENT MODPACKS --

                passiveItem {
                    Row(
                        Modifier.padding(start = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            "Recent Modpacks",
                            color = Color.Gray,
                        )
                    }
                }

                profileData.recentProfilesFiltered.map { profile ->
                    selectableItem(false, onClick = {
                        if (modpackUiState.action.first != null)
                        {
                            ProfileViewModel.updateCloseDialog {
                                ProfileViewModel.updateCurrentProfile(Path(profile.path))
                            }
                        }
                        else
                        {
                            coroutineScope.launch {
                                ProfileViewModel.updateCurrentProfile(Path(profile.path))
                            }
                        }
                    }) {
                        Row(
                            Modifier.padding(2.dp)
                        ) {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column {
                                Text(
                                    profile.name,
                                    color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
