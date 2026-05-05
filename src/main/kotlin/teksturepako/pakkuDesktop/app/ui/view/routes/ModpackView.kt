/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.getError
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.titlebar.AlignedTitleBarContent
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.button.SettingsButton
import teksturepako.pakkuDesktop.app.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkuDesktop.app.ui.component.modpack.ModpackSideBar
import teksturepako.pakkuDesktop.app.ui.driver.LocalPickDirectory
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.GitTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ModpackTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectsTab
import teksturepako.pakkuDesktop.pkui.component.toast.ToastHost
import teksturepako.pakkuDesktop.pro.ui.component.GitDropdown
import teksturepako.pakkuDesktop.pro.ui.component.Pro

@Composable
fun PakkuApplicationScope.ModpackView(
    publish: (AppMsg) -> Unit,
    model: AppModel,
) {
    val titleBarHeight = 40.dp
    val modpack = model.modpack
    val pickDirectory = LocalPickDirectory.current

    // React to FileNotFound error — show NewModpack dialog
    LaunchedEffect(modpack.lockFile) {
        if (modpack.lockFile?.getError() is FileNotFound) {
            publish(AppMsg.ShowNewModpack)
        }
    }

    // React to other errors
    val hasNonFileNotFoundError = modpack.lockFile?.isErr == true &&
        modpack.lockFile.getError() !is FileNotFound

    val actionSplitState = remember { org.jetbrains.jewel.ui.component.SplitLayoutState(1f) }

    MainTitleBar(Modifier.height(titleBarHeight), withGradient = true) {
        AlignedTitleBarContent(alignment = Alignment.Start) {
            ModpackDropdown(publish, model, onOpenDirectory = { pickDirectory() })
            Pro { GitDropdown() }
            if (modpack.actionName != null) {
                Box(Modifier.padding(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(modpack.actionName)
                        CircularProgressIndicator()
                    }
                }
            }
        }
        AlignedTitleBarContent(alignment = Alignment.End) {
            SettingsButton(onClick = { publish(AppMsg.ShowSettings) })
        }
    }

    if (hasNonFileNotFoundError) {
        // Error dialog will be shown by AppView
        return
    }

    val toastState = remember { mutableStateOf(modpack.toasts) }
    LaunchedEffect(modpack.toasts) { toastState.value = modpack.toasts }

    Row(Modifier.fillMaxSize().subtractTopHeight(titleBarHeight)) {
        ModpackSideBar(publish, modpack)

        VerticalSplitLayout(
            state = actionSplitState,
            first = {
                Column {
                    Row {
                        when (modpack.selectedTab) {
                            SelectedTab.PROJECTS -> ProjectsTab(publish, model)
                            SelectedTab.MODPACK  -> ModpackTab()
                            SelectedTab.COMMIT   -> GitTab()
                        }
                    }
                }
            },
            second = {
                Column { Row { } }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            firstPaneMinWidth = 100.dp,
            secondPaneMinWidth = 40.dp,
            draggableWidth = 16.dp
        )
    }

    ToastHost(
        toasts = toastState,
        modifier = Modifier.fillMaxSize().subtractTopHeight(titleBarHeight),
        alignment = Alignment.TopEnd,
        spacing = 8.dp,
        onDismiss = { id -> publish(AppMsg.Modpack.ToastDismissed(id)) },
    )

}
