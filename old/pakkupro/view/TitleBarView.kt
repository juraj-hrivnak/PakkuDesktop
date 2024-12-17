package pakkupro.view

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import teksturepako.pakkupro.data.ProfileData
import pakkupro.viewmodel.MainViewModel
import pakkupro.viewmodel.PakkuDesktopIcons
import pakkupro.viewmodel.WindowType
import teksturepako.pakkupro.ui.application.appName
import kotlin.io.path.Path

@Composable
fun DecoratedWindowScope.TitleBarView(
    enabled: Boolean = true,
    filePicker: PickerResultLauncher,
    windowType: MutableState<WindowType>,
    exporting: MutableState<Boolean>
)
{
    val height = 33.dp

    val coroutineScope = rememberCoroutineScope()

    TitleBar(
        Modifier.newFullscreenControls().height(height),
        gradientStartColor = Color(16, 77, 69),
    ) {

        // -- APP ICON --

        Row(
            Modifier
                .fillMaxHeight()
                .align(Alignment.Start)
                .width(25.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PakkuDesktopIcons.pakku,
                "pakku",
                Modifier
                    .offset(x = 8.dp)
                    .size(25.dp)
            )
        }

        // -- FILE DROPDOWN --

        /* SPACE */ Row(
            Modifier
                .fillMaxHeight()
                .align(Alignment.Start)
                .width(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

        }

        Dropdown(
            enabled = enabled,
            modifier = Modifier
                .padding(start = 10.dp)
                .align(Alignment.Start),
            content = {
                Row(
                    Modifier
                        .fillMaxHeight(0.9f)
                        .align(Alignment.Start),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainViewModel.configFile?.getName()?.let {
                        Text(it, Modifier.align(Alignment.Start))
                    }
                }
            },
            menuModifier = Modifier
                .offset(x = 12.dp)
                .width(160.dp),
            menuContent = {

                // -- NEW --

//                selectableItem(false, onClick = {
//
//                }) {
//                    Row {
//                        Column(Modifier.fillMaxWidth(0.2f)) {}
//                        Column {
//                            Text(
//                                "New Modpack",
//                                Modifier,
//                                color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
//                            )
//                        }
//                    }
//                }
//
//                // -- IMPORT --
//
//                selectableItem(false, onClick = {
//
//                }) {
//                    Row {
//                        Column(Modifier.fillMaxWidth(0.2f)) {}
//                        Column {
//                            Text(
//                                "Import...",
//                                Modifier,
//                                color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
//                            )
//                        }
//                    }
//                }

                // -- OPEN --

                selectableItem(false, onClick = {
                    filePicker.launch()
                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(
                                key = PakkuDesktopIcons.open,
                                contentDescription = "open",
                                modifier = Modifier.size(15.dp),
                                tint = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                            )
                        }
                        Column {
                            Text(
                                "Open...",
                                Modifier,
                                color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                            )
                        }
                    }
                }

                separator()

                // -- EXPORT --

                selectableItem(
                    selected = false,
                    enabled = !exporting.value,
                    onClick = {
                        coroutineScope.launch {
                            exporting.value = true
                        }
                    },
                ) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(
                                key = PakkuDesktopIcons.cube,
                                "export",
                                modifier = Modifier.size(15.dp),
                                tint = if (exporting.value)
                                {
                                    Color.Gray
                                }
                                else
                                {
                                    if (MainViewModel.theme.isDark()) Color.White else Color.Black
                                }
                            )
                        }
                        Column {
                            Text(
                                "Export",
                                Modifier,
                                color = if (exporting.value)
                                {
                                    Color.Gray
                                }
                                else
                                {
                                    if (MainViewModel.theme.isDark()) Color.White else Color.Black
                                }
                            )
                        }
                    }
                }

                separator()

                // -- FETCH --

                selectableItem(false, onClick = {

                }) {
                    Row {
                        Column(Modifier.fillMaxWidth(0.2f)) {
                            Icon(
                                key = PakkuDesktopIcons.cloudDownload,
                                contentDescription = "fetch",
                                Modifier.size(15.dp),
                                tint = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                            )
                        }
                        Column {
                            Text(
                                "Fetch",
                                Modifier,
                                color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                            )
                        }
                    }
                }

                if (ProfileData.readOrNew()?.recentProfiles?.isNotEmpty() == true)
                {
                    separator()

                    // -- RECENT MODPACKS --

                    passiveItem {
                        Row(
                            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Recent Modpacks",
                                color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                            )
                        }
                    }

                    ProfileData.readOrNew().recentProfiles.forEach { (modpack, path) ->
                        selectableItem(false, onClick = {
                            MainViewModel.updatePakkuApi()
                            windowType.value = WindowType.LOADING

                            MainViewModel.profileData.setCurrentProfile(Path(path))
                            MainViewModel.profileData.write()
                        }) {
                            Row {
                                Column(Modifier.fillMaxWidth(0.2f)) {}
                                Column {
                                    Text(
                                        modpack, color = if (MainViewModel.theme.isDark()) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        if (exporting.value)
        {
            Row(
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.Start)
                    .width(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {}

            Row(
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.Start)
                    .width(10.dp)
                    .size(25.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(30.dp))
            }
        }

        // -- APP TITLE --

        Row(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(appName())
        }

        // -- THEME BUTTONS --

        Row(
            Modifier.fillMaxHeight().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (MainViewModel.theme.isDark())
            {
                IconButton(
                    onClick = {
                        ProfileData.readOrNew()?.run {
                            setTheme(IntUiThemes.Light)
                            write()
                        }
                        MainViewModel.theme = IntUiThemes.Light
                    },
                    Modifier.size(30.dp),
                    enabled = enabled
                ) {
                    Icon(
                        key = PakkuDesktopIcons.darkTheme,
                        contentDescription = "dark_theme"
                    )
                }
            }
            else
            {
                IconButton(
                    onClick = {
                        ProfileData.readOrNew()?.run {
                            setTheme(IntUiThemes.Dark)
                            write()
                        }
                        MainViewModel.theme = IntUiThemes.Dark
                    },
                    Modifier.size(30.dp),
                    enabled = enabled
                ) {
                    Icon(
                        key = PakkuDesktopIcons.lightTheme,
                        contentDescription = "light_theme"
                    )
                }
            }
        }
    }
}