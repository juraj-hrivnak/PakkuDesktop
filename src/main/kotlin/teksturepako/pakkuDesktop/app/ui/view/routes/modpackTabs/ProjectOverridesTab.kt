/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Size
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSelection
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldValues
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackIsland
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.write
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectFilterBar
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FloatingActionIcon
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.ProjectListQuery
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.matchesProjectListQuery
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectOverridesTabModel
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.DropdownHost
import teksturepako.pakkuDesktop.pkui.component.IslandCard
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

@Composable
fun ProjectOverridesTab(publish: (ModpackMsg) -> Unit, model: ProjectOverridesTabModel) {
    RecomposeProbe("ProjectOverridesTab")
    DropdownHost {
        ProjectOverridesTabBody(publish, model)
    }
}

@Composable
private fun ProjectOverridesTabBody(publish: (ModpackMsg) -> Unit, model: ProjectOverridesTabModel) {
    RecomposeProbe("ProjectOverridesTabBody")
    val config = model.configFile?.get()
    val lock = model.lockFile?.get()
    if (config == null || lock == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Load a modpack to edit project overrides.",
                color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
            )
        }
        return
    }

    val values = remember(config, lock, model.configText?.text) {
        ModpackFieldValues(config, lock, model.configText?.unknownKeys.orEmpty())
    }

    val projectsField = ModpackFieldSchema.fields.firstOrNull { it.hint.projectCard }
        ?: return

    val scrollState = rememberScrollState()
    var pickingOverrides by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = PakkuDesktopConstants.commonPaddingSize, end = 8.dp),
        ) {
            val overridesField = projectsField
            val overridesValue = values[overridesField] as? JsonObject ?: JsonObject(emptyMap())

            IslandCard(
                title = ModpackIsland.Projects.title,
                description = ModpackIsland.Projects.description,
                modifier = Modifier.fillMaxWidth(),
                trailing = {
                    FloatingActionIcon(
                        onClick = { pickingOverrides = true },
                        enabled = true,
                        buttonSize = 36.dp,
                        accent = PakkuDesktopConstants.highlightColor,
                        tooltip = "Add project override",
                    ) { hovered ->
                        Icon(
                            key = AllIconsKeys.General.Add,
                            contentDescription = "Add project override",
                            tint = if (hovered) PakkuDesktopConstants.highlightColor else JewelTheme.contentColor,
                            hints = arrayOf(Size(18)),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            ) {
                if (overridesValue.isEmpty()) {
                    Text(
                        "No project overrides yet.",
                        color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
                    )
                } else {
                    ModpackFieldSelection(
                        field = overridesField,
                        value = values[overridesField],
                        errors = model.metaErrors[overridesField.key]?.orEmpty() ?: emptyList(),
                        onEdit = { next -> overridesField.write(next) },
                        publish = publish,
                        projectOf = { key -> lock.getProject(key) },
                        metaErrors = model.metaErrors,
                    )
                }
            }
        }
        VerticalScrollbar(
            scrollState,
            modifier = Modifier.fillMaxHeight(),
        )
    }

    if (pickingOverrides) {
        AddOverrideDialog(
            projects = lock.getAllProjects(),
            already = (values[projectsField] as? JsonObject)?.keys.orEmpty(),
            onDismiss = { pickingOverrides = false },
            onAdd = { keys: List<String> ->
                val current = values[projectsField] as? JsonObject ?: JsonObject(emptyMap())
                val ordered = linkedMapOf<String, JsonElement>()
                for (key in keys) {
                    if (key !in ordered) ordered[key] = JsonObject(emptyMap())
                }
                for ((key, value) in current) {
                    if (key !in ordered) ordered[key] = value
                }
                publish(ModpackMsg.MetaWriteRequested(projectsField.write(JsonObject(ordered))))
                pickingOverrides = false
            },
        )
    }
}

@Composable
private fun AddOverrideDialog(
    projects: List<Project>,
    already: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    RecomposeProbe("AddOverrideDialog")
    val available = projects.mapNotNull { project ->
        val key = project.slug.values.firstOrNull() ?: project.pakkuId ?: project.name.values.firstOrNull()
        if (key == null || already.any { it in project }) null else key to project
    }
    var query by remember { mutableStateOf(ProjectListQuery()) }
    val visible = available.filter { (_, project) -> project.matchesProjectListQuery(query) }
    var chosen by remember { mutableStateOf(listOf<String>()) }
    val searchFocus = remember { FocusRequester() }
    PkUiDialog(
        onDismiss = onDismiss,
        title = "Add project override",
        subtitle = "Pick projects already in this modpack. They stay installed; this only adds a property override.",
        icon = AllIconsKeys.General.Add,
        size = PkUiDialogSize.Large,
        scrollable = false,
        initialFocus = searchFocus,
        onConfirm = if (chosen.isEmpty()) null else { { onAdd(chosen) } },
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            DefaultButton(onClick = { onAdd(chosen) }, enabled = chosen.isNotEmpty()) { Text("Add") }
        },
    ) {
        ProjectFilterBar(
            query = query,
            onQuery = { query = it },
            focusRequester = searchFocus,
        )
        when {
            available.isEmpty() -> Text(
                "Every project already has an override.",
                color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
            )
            visible.isEmpty() -> Text(
                "No projects match.",
                color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
            )
            else -> {
                val listScroll = rememberScrollState()
                Box(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(listScroll)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing),
                    ) {
                        visible.forEach { (key, project) ->
                            val checked = key in chosen
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        chosen = if (on) listOf(key) + (chosen - key) else chosen - key
                                    },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(project.name.values.firstOrNull() ?: key, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                    Text(
                                        project.type.prettyName,
                                        color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
                                        fontSize = PakkuDesktopConstants.cardCaptionSize,
                                    )
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        listScroll,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
