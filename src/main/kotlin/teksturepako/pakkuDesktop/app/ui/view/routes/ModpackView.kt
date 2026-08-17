/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.application.titlebar.AlignedTitleBarContent
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.button.SettingsButton
import teksturepako.pakkuDesktop.app.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkuDesktop.app.ui.component.modpack.ModpackRail
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.GitTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ModpackTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectsTab
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.pkui.component.DropdownHost
import teksturepako.pakkuDesktop.pkui.component.toast.ToastHost
import teksturepako.pakkuDesktop.pro.ui.component.GitDropdown
import teksturepako.pakkuDesktop.pro.ui.component.Pro

@Composable
fun PakkuApplicationScope.ModpackView(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    appModel: AppModel,
    appPublish: (AppMsg) -> Unit,
) {
    val profileData = appModel.profile.data
    val titleBarHeight = 40.dp

    // FileNotFound → ShowNewModpack is handled by modpackDiskDriver, not here.
    val hasNonFileNotFoundError =
        (model.lockFile?.isErr == true && model.lockFile.getError() !is FileNotFound) ||
            (model.configFile?.isErr == true && model.configFile.getError() !is FileNotFound)

    val actionSplitState = remember { org.jetbrains.jewel.ui.component.SplitLayoutState(1f) }

    MainTitleBar(
        Modifier.height(titleBarHeight),
        withGradient = true,
        themeTrailingActions = {
            ThemeButton(appPublish, profileData.intUiTheme)
        },
    ) {
        AlignedTitleBarContent(alignment = Alignment.Start) {
            DropdownHost {
                ModpackDropdown(publish, model)
                Pro(appModel) { GitDropdown(publish, model) }
            }
            McLoaderLabel(model)
            if (model.actionName != null) {
                Box(Modifier.padding(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(model.actionName)
                        CircularProgressIndicator()
                    }
                }
            }
        }
        AlignedTitleBarContent(alignment = Alignment.End) {
            SettingsButton(onClick = { publish(ModpackMsg.ShowSettings) })
        }
    }

    if (hasNonFileNotFoundError) return

    val toastState = remember { mutableStateOf(model.toasts) }
    toastState.value = model.toasts

    Box(Modifier.fillMaxSize().subtractTopHeight(titleBarHeight)) {
        Row(Modifier.matchParentSize()) {
            ModpackRail(publish, model, appModel)

            VerticalSplitLayout(
                state = actionSplitState,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Column {
                        Row {
                            when (model.selectedTab) {
                                SelectedTab.PROJECTS -> ProjectsTab(publish, model)
                                SelectedTab.MODPACK  -> ModpackTab(publish, model)
                                SelectedTab.COMMIT   -> GitTab(publish, model)
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
            modifier = Modifier.align(Alignment.TopEnd),
            alignment = Alignment.TopEnd,
            spacing = 8.dp,
            onDismiss = { id -> publish(ModpackMsg.ToastDismissed(id)) },
        )
    }
}

@Composable
private fun McLoaderLabel(model: ModpackModel)
{
    val lockFile = model.lockFile?.get() ?: return
    val mc = lockFile.getFirstMcVersion() ?: return
    val loader = lockFile.getLoaders().firstOrNull()
    val label = if (loader != null) "MC $mc · $loader" else "MC $mc"
    Text(
        label,
        color = JewelTheme.contentColor.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}
