/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.github.michaelbull.result.getError
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.titlebar.AlignedTitleBarContent
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.Error
import teksturepako.pakkuDesktop.app.ui.component.button.SettingsButton
import teksturepako.pakkuDesktop.app.ui.component.dialog.CreateModpackDialog
import teksturepako.pakkuDesktop.app.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkuDesktop.app.ui.component.modpack.ModpackSideBar
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.app.ui.view.Navigation
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.GitTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ModpackTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectsTab
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.SelectedTab
import teksturepako.pakkuDesktop.pkui.component.toast.ToastHost
import teksturepako.pakkuDesktop.pro.ui.component.GitDropdown
import teksturepako.pakkuDesktop.pro.ui.component.Pro
import kotlin.io.path.Path

@Composable
fun PakkuApplicationScope.ModpackView(navController: NavHostController)
{
    val titleBarHeight = 40.dp

    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val profileData by ProfileViewModel.profileData.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lifecycleState, profileData.currentProfile)
    {
        ModpackViewModel.loadFromDisk()
    }

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
        platformSettings = FileKitPlatformSettings(parentWindow = this.decoratedWindowScope.window)
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        if (modpackUiState.action.first != null)
        {
            ProfileViewModel.updateCloseDialog {
                ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
                navController.navigate(Navigation.Modpack.route)
            }
        }
        else
        {
            coroutineScope.launch {
                ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
                navController.navigate(Navigation.Modpack.route)
            }
        }
    }

    MainTitleBar(Modifier.height(titleBarHeight), withGradient = true) {
        AlignedTitleBarContent(alignment = Alignment.Start) {
            ModpackDropdown(pickerLauncher, navController)
            Pro { GitDropdown() }
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
        AlignedTitleBarContent(alignment = Alignment.End) {
            SettingsButton(onClick = { navController.navigate(Navigation.Settings(Navigation.Modpack).route) })
        }
    }

    if (modpackUiState.lockFile?.isErr == true)
    {
        Row(
            Modifier
                .fillMaxSize()
                .subtractTopHeight(titleBarHeight)
        ) {
            if (modpackUiState.lockFile!!.getError() is FileNotFound)
            {
                CreateModpackDialog()
            }
            else
            {
                modpackUiState.lockFile!!.getError()?.let { Error(it) }
            }
        }

        return
    }

    Row(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight)
    ) {
        ModpackSideBar()

        VerticalSplitLayout(
            state = ModpackViewModel.actionSplitState,
            first = {
                Column {
                    Row {
                        when (modpackUiState.selectedTab)
                        {
                            SelectedTab.PROJECTS -> ProjectsTab()
                            SelectedTab.MODPACK  -> ModpackTab()
                            SelectedTab.COMMIT   -> GitTab()
                        }
                    }
                }
            },
            second = {
                Column {
                    Row {
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 100.dp,
            secondPaneMinWidth = 40.dp,
            draggableWidth = 16.dp
        )

    }

    ToastHost(
        ModpackViewModel.toasts,
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight),
        alignment = Alignment.TopEnd,
        spacing = 8.dp
    )

}
