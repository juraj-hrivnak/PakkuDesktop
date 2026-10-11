/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.actions.AppToastContent
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.component.dialog.AddProjectsDialog
import teksturepako.pakkuDesktop.app.ui.component.dialog.GitCommitDialog
import teksturepako.pakkuDesktop.app.ui.component.dialog.GitPullDialog
import teksturepako.pakkuDesktop.app.ui.component.dialog.RemoveProjectsDialog
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
import teksturepako.pakkuDesktop.app.ui.model.filesTab
import teksturepako.pakkuDesktop.app.ui.model.gitTab
import teksturepako.pakkuDesktop.app.ui.model.modpackTab
import teksturepako.pakkuDesktop.app.ui.model.projectsTab
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.FilesTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.GitTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ModpackTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectsTab
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.pkui.component.DropdownHost
import teksturepako.pakkuDesktop.pkui.component.toast.ToastHost
import teksturepako.pakkuDesktop.pro.ui.component.GitDropdown
import teksturepako.pakkuDesktop.pro.ui.component.GitPushDialog

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
    val lockFile = model.lockFile
    val configFile = model.configFile
    val hasNonFileNotFoundError =
        (lockFile?.isErr == true && lockFile.getError() !is FileNotFound) ||
            (configFile?.isErr == true && configFile.getError() !is FileNotFound)

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
                GitDropdown(publish, model)
            }
            McLoaderLabel(model)
            val actionName = model.actionName
            if (actionName != null) {
                Box(Modifier.padding(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(actionName)
                        val total = model.fetchProgressTotal
                        if (model.actionName == "Fetching" && total > 0L) {
                            val progress = (model.fetchProgressCompleted.toFloat() / total.toFloat())
                                .coerceIn(0f, 1f)
                            HorizontalProgressBar(progress, Modifier.width(120.dp))
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        AlignedTitleBarContent(alignment = Alignment.End) {
            SettingsButton(onClick = { publish(ModpackMsg.ShowSettings) })
        }
    }

    if (hasNonFileNotFoundError) return

    Box(Modifier.fillMaxSize().subtractTopHeight(titleBarHeight)) {
        Row(Modifier.matchParentSize()) {
            ModpackRail(publish, model.selectedTab)

            VerticalSplitLayout(
                state = actionSplitState,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxSize()) {
                            when (model.selectedTab) {
                                SelectedTab.PROJECTS -> ProjectsTab(publish, model.projectsTab())
                                SelectedTab.FILES    -> FilesTab(publish, model.filesTab())
                                SelectedTab.MODPACK  -> ModpackTab(publish, model.modpackTab())
                                SelectedTab.COMMIT   -> GitTab(publish, model.gitTab())
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
            toasts = model.toasts,
            modifier = Modifier.align(Alignment.TopEnd),
            alignment = Alignment.TopEnd,
            spacing = 8.dp,
            onDismiss = { id -> publish(ModpackMsg.ToastDismissed(id)) },
        ) { body -> AppToastContent(body) }

        AddProjectsDialog(publish = publish, model = model)
        RemoveProjectsDialog(publish = publish, model = model)
        GitCommitDialog(publish = publish, model = model)
        GitPullDialog(publish = publish, model = model)
        GitPushDialog(publish = publish, model = model)
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
