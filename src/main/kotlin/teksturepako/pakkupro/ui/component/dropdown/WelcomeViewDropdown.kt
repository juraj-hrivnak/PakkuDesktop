package teksturepako.pakkupro.ui.component.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import kotlin.io.path.Path

@Composable
fun WelcomeViewDropdown(
    pickerLauncher: PickerResultLauncher,
)
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    Dropdown(
        Modifier.padding(vertical = 4.dp),
        content = {
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("File")
            }
        },
        menuModifier = Modifier
            .offset(x = 12.dp)
            .width(160.dp),
        menuContent = {

            // -- OPEN --

            selectableItem(false, onClick = {
                pickerLauncher.launch()
            }) {
                Row {
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

            if (profileData.recentProfilesFiltered.isNotEmpty())
            {
                separator()

                // -- RECENT MODPACKS --

                passiveItem {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Recent Modpacks",
                            color = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                }

                profileData.recentProfilesFiltered.forEach { (modpack, path) ->
                    selectableItem(false, onClick = {
                        ProfileViewModel.updateCurrentProfile(Path(path))
                    }) {
                        Row {
                            Column(Modifier.fillMaxWidth(0.2f)) {}
                            Column {
                                Text(
                                    modpack,
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
