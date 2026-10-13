/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.context

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.ui.component.ContextMenuItemOption
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.integration.openExternalUrl
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectUpdateInfo
import teksturepako.pakkuDesktop.pkui.component.PkUiContextMenuArea

fun projectContextMenuItems(
    project: Project,
    publish: (ModpackMsg) -> Unit,
    busy: Boolean,
    updatePreviews: Map<String, ProjectUpdateInfo>?,
): List<ContextMenuItem> {
    val key = project.uiKey()
    val statusChecked = updatePreviews != null
    val updateInfo = updatePreviews?.get(key)
    val canUpdate = statusChecked && updateInfo != null && !updateInfo.applied
    val checkMode = !statusChecked

    return buildList {
        add(
            ContextMenuItemOption(
                label = "View in inspector",
                icon = AllIconsKeys.Toolwindows.ToolWindowProject,
                enabled = !busy,
                action = {
                    publish(ModpackMsg.ProjectSelected(project))
                },
            ),
        )
        add(
            ContextMenuItemOption(
                label = if (checkMode) "Check for updates" else "Update project",
                icon = if (checkMode) AllIconsKeys.Actions.Refresh else AllIconsKeys.Actions.CheckOut,
                enabled = !busy && (checkMode || canUpdate),
                action = {
                    publish(ModpackMsg.ProjectsSelected(setOf(key)))
                    if (checkMode) publish(ModpackMsg.StatusCheckRequested)
                    else publish(ModpackMsg.UpdateRequested(setOf(key)))
                },
            ),
        )
        add(
            ContextMenuItemOption(
                label = "Remove from modpack…",
                icon = AllIconsKeys.General.Delete,
                enabled = !busy,
                action = {
                    publish(ModpackMsg.ProjectsSelected(setOf(key)))
                    publish(ModpackMsg.ShowRemoveDialog)
                },
            ),
        )
        for (provider in project.getProviders()) {
            val slug = project.slug[provider.serialName] ?: continue
            val webUrl = when (provider) {
                is Platform -> "${provider.getUrlForProjectType(project.type)}/$slug"
                else -> provider.siteUrl?.let { "$it/$slug" }
            } ?: continue
            add(
                ContextMenuItemOption(
                    label = "Open on ${provider.shortName.uppercase()}",
                    icon = AllIconsKeys.Nodes.PpWeb,
                    action = { openExternalUrl(webUrl) },
                ),
            )
        }
    }
}

@Composable
fun ProjectRowContextMenu(
    project: Project,
    publish: (ModpackMsg) -> Unit,
    busy: Boolean,
    updatePreviews: Map<String, ProjectUpdateInfo>?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PkUiContextMenuArea(
        modifier = modifier,
        items = { projectContextMenuItems(project, publish, busy, updatePreviews) },
        content = { Box(Modifier.fillMaxWidth()) { content() } },
    )
}
