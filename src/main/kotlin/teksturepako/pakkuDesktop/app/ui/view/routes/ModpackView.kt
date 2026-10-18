/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import org.jetbrains.jewel.ui.theme.badgeStyle
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.actions.AppToastContent
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.dialog.AddProjectsDialog
import teksturepako.pakkuDesktop.app.ui.component.dialog.RemoveProjectsDialog
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BASE_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BORDER_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.LocalTitleBarWidthTier
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TitleBarStartRow
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TitleBarWidthTier
import teksturepako.pakkuDesktop.app.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkuDesktop.app.ui.component.modpack.ModpackRail
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.model.filesTab
import teksturepako.pakkuDesktop.app.ui.model.gitTab
import teksturepako.pakkuDesktop.app.ui.model.modpackTab
import teksturepako.pakkuDesktop.app.ui.model.overridesTab
import teksturepako.pakkuDesktop.app.ui.model.projectsTab
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.app.ui.component.git.log.GitLogView
import teksturepako.pakkuDesktop.app.ui.model.logTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.FilesTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.GitTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ModpackTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectOverridesTab
import teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs.ProjectsTab
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.pkui.component.DropdownHost
import teksturepako.pakkuDesktop.pkui.component.toast.ToastHost
import teksturepako.pakkuDesktop.app.ui.component.git.GitDropdown
import teksturepako.pakkuDesktop.app.ui.component.git.GitDialogs

@Composable
fun PakkuApplicationScope.ModpackView(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    appModel: AppModel,
) {
    val titleBarHeight = TITLE_BAR_BASE_HEIGHT_DP.dp
    val titleBarChromeHeight = titleBarHeight + TITLE_BAR_BORDER_HEIGHT_DP.dp

    // FileNotFound → ShowNewModpack is handled by modpackDiskDriver, not here.
    val lockFile = model.lockFile
    val configFile = model.configFile
    val hasNonFileNotFoundError =
        (lockFile?.isErr == true && lockFile.getError() !is FileNotFound) ||
            (configFile?.isErr == true && configFile.getError() !is FileNotFound)

    val actionSplitState = remember { org.jetbrains.jewel.ui.component.SplitLayoutState(1f) }

    if (hasNonFileNotFoundError) return

    Box(Modifier.fillMaxSize().subtractTopHeight(titleBarChromeHeight)) {
        Row(Modifier.matchParentSize()) {
            ModpackRail(publish, model.selectedTab)

            VerticalSplitLayout(
                state = actionSplitState,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxSize()) {
                            when (model.selectedTab) {
                                SelectedTab.PROJECTS  -> ProjectsTab(publish, model.projectsTab())
                                SelectedTab.PROJECT_OVERRIDES -> ProjectOverridesTab(publish, model.overridesTab())
                                SelectedTab.FILES     -> FilesTab(publish, model.filesTab())
                                SelectedTab.MODPACK   -> ModpackTab(publish, model.modpackTab())
                                SelectedTab.COMMIT    -> GitTab(
                                    publish,
                                    model.gitTab(),
                                    appModel.profile.data.currentProfilePath,
                                )
                                SelectedTab.LOG      -> GitLogView(
                                    publish,
                                    model.logTab(),
                                    Modifier.fillMaxSize(),
                                )
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
        GitDialogs(
            publish = publish,
            model = model,
            directory = appModel.profile.data.currentProfile?.path,
        )
    }
}

@Composable
fun ModpackTitleBarStart(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val widthTier = LocalTitleBarWidthTier.current
    TitleBarStartRow {
        DropdownHost {
            ModpackDropdown(publish, model)
            GitDropdown(publish, model)
        }
        if (widthTier == TitleBarWidthTier.Comfortable) {
            McLoaderLabel(model)
        }
        val actionName = model.actionName
        if (actionName != null) {
            Row(
                Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    actionName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                )
                val total = model.fetchProgressTotal
                if (model.actionName == "Fetching" && total > 0L) {
                    val progress = (model.fetchProgressCompleted.toFloat() / total.toFloat())
                        .coerceIn(0f, 1f)
                    HorizontalProgressBar(
                        progress,
                        Modifier.width(if (widthTier == TitleBarWidthTier.Comfortable) 120.dp else 64.dp),
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun McLoaderLabel(model: ModpackModel) {
    val lockFile = model.lockFile?.get() ?: return
    val mcVersions = lockFile.getMcVersions()
    val loaders = lockFile.getLoadersWithVersions()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mcVersions.firstOrNull()?.let { mc ->
            val more = mcVersions.size - 1
            Badge(style = JewelTheme.badgeStyle.graySecondary) {
                Text(if (more > 0) "$mc +$more" else mc)
            }
        }
        loaders.forEach { (loader, version) ->
            Badge(style = JewelTheme.badgeStyle.graySecondary) {
                Text(if (version.isNotBlank()) "$loader $version" else loader)
            }
        }
    }
}
