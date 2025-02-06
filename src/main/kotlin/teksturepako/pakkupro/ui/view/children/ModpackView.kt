package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkupro.io.RevealFileAction
import teksturepako.pakkupro.actions.ExportData
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkupro.ui.component.modpack.ModpackSideBar
import teksturepako.pakkupro.ui.modifier.subtractTopHeight
import teksturepako.pakkupro.ui.view.children.modpackTabs.ModpackTab
import teksturepako.pakkupro.ui.view.children.modpackTabs.ProjectsTab
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab
import kotlin.io.path.Path
import kotlin.io.path.pathString

@Composable
fun PakkuApplicationScope.ModpackView()
{
    val titleBarHeight = 40.dp

    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val profileData by ProfileViewModel.profileData.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    coroutineScope.launch {
        ModpackViewModel.loadFromDisk()
    }

    ModpackViewModel.toasterState = rememberToasterState()

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
        platformSettings = FileKitPlatformSettings(parentWindow = this.decoratedWindowScope.window)
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        if (modpackUiState.action.first != null)
        {
            ProfileViewModel.updateCloseDialog {
                ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
            }
        }
        else
        {
            coroutineScope.launch {
                ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
            }
        }
    }

    MainTitleBar(Modifier.height(titleBarHeight), withGradient = true) {
        ModpackDropdown(pickerLauncher)
        if (modpackUiState.action.first != null)
        {
            Box(Modifier.padding(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(modpackUiState.action.first!!)
                    CircularProgressIndicator()
                }
            }
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .offset(y = titleBarHeight)
    ) {
        ModpackViewModel.toasterState?.let {
            Toaster(
                state = it,
                alignment = Alignment.BottomEnd,
                darkTheme = profileData.intUiTheme.isDark(),
                expanded = true,
                richColors = true,
                showCloseButton = true,
                maxVisibleToasts = 6,
                messageSlot = { toast ->
                    if (toast.message is ExportData)
                    {
                        val toastData = toast.message as ExportData

                        Column {
                            Row {
                                Text("[${toastData.profile.name} profile]", fontWeight = FontWeight.Bold)
                                Text(" exported to:")
                            }
                            Row {
                                Text(
                                    toastData.path.pathString,
                                    Modifier.clickable {
                                        RevealFileAction.openFile(toastData.path)
                                    },
                                    style = JewelTheme.editorTextStyle,
                                )
                            }
                        }
                    }
                    else
                    {
                        Text(toast.message.toString())
                    }
                },
                background = { SolidColor(JewelTheme.globalColors.panelBackground) },
            )
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight)
    ) {
        ModpackSideBar()

        when (modpackUiState.selectedTab)
        {
            SelectedTab.PROJECTS -> ProjectsTab()
            SelectedTab.MODPACK  -> ModpackTab()
        }
    }
}
