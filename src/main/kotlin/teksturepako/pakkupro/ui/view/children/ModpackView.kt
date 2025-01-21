package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.HorizontalBar
import teksturepako.pakkupro.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkupro.ui.component.modpack.ModpackSideBar
import teksturepako.pakkupro.ui.component.modpack.ProjectDisplay
import teksturepako.pakkupro.ui.component.modpack.ProjectFilter
import teksturepako.pakkupro.ui.component.modpack.ProjectsList
import teksturepako.pakkupro.ui.view.children.modpackTabs.ModpackTab
import teksturepako.pakkupro.ui.view.children.modpackTabs.ProjectsTab
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab
import kotlin.io.path.Path

@Composable
fun PakkuApplicationScope.ModpackView()
{
    val titleBarHeight = 40.dp

    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    coroutineScope.launch {
        ModpackViewModel.loadFromDisk()
    }

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory"
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
    }

    MainTitleBar(Modifier.height(titleBarHeight), withGradient = true) {
        ModpackDropdown(pickerLauncher)
    }

    Row(
        Modifier
            .fillMaxSize()
            .offset(y = titleBarHeight),
    ) {
        ModpackSideBar()

        when (modpackUiState.selectedTab)
        {
            SelectedTab.PROJECTS -> ProjectsTab()
            SelectedTab.MODPACK  -> ModpackTab()
        }
    }
}
