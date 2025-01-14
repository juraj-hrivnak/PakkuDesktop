package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.dropdown.WelcomeViewDropdown
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import kotlin.io.path.Path

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakkuApplicationScope.WelcomeView()
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    val titleBarHeight = 40.dp

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
        platformSettings = FileKitPlatformSettings(parentWindow = this.decoratedWindowScope.window)
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
    }

    MainTitleBar(Modifier.height(titleBarHeight)) {
        Text("Welcome to Pakku Pro")
        WelcomeViewDropdown(pickerLauncher)
    }

    Column(
        Modifier
            .fillMaxSize()
            .offset(y = titleBarHeight),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowColumn(
                verticalArrangement = Arrangement.Center,
            ) {
                GradientHeader("Welcome to Pakku Pro!")
            }
        }

        val scrollState = rememberScrollState()

        Row(
            Modifier
                .padding(PakkuDesktopConstants.commonPaddingSize)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Header("Recent Modpacks")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(PakkuDesktopConstants.commonPaddingSize)
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                profileData.recentProfilesFiltered.forEach { (modpack, path) ->
                    OutlinedButton(
                        onClick = {
                            ProfileViewModel.updateCurrentProfile(Path(path))
                        },
                    ) {
                        Text(
                            modpack,
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        VerticalScrollbar(scrollState = scrollState)
    }
}

