/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Size
import org.jetbrains.jewel.ui.theme.badgeStyle
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.git.diff.DiffViewer
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectFilterBar
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FloatingActionIcon
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.ProjectListQuery
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.matchesProjectListQuery
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackIsland
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.text
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.textValue
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.write
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSelection
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldValues
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.summaryLabels
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
import teksturepako.pakkuDesktop.app.ui.model.OverviewMode
import teksturepako.pakkuDesktop.app.ui.model.metaFieldErrors
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.pkui.component.DropdownHost
import teksturepako.pakkuDesktop.pkui.component.InlineEditText
import teksturepako.pakkuDesktop.pkui.component.IslandCard
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize

@Composable
fun ModpackTab(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    RecomposeProbe("ModpackTab")
    DropdownHost {
        ModpackTabBody(publish, model)
    }
}

@Composable
private fun ModpackTabBody(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    RecomposeProbe("ModpackTabBody")
    val config = model.configFile?.get()
    val lock = model.lockFile?.get()
    if (config == null || lock == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Load a modpack to edit metadata.",
                color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
            )
        }
        return
    }

    val values = remember(config, lock, model.configText) {
        ModpackFieldValues(config, lock, model.configText?.unknownKeys.orEmpty())
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                .padding(top = PakkuDesktopConstants.commonPaddingSize, bottom = 2.dp),
        ) {
            OverviewHeader(publish, model, values)
        }
        Divider(Orientation.Horizontal)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (model.overviewMode == OverviewMode.Json) {
                OverviewJson(model)
            } else {
                OverviewIslands(publish, model, values, lock.getAllProjects()) { key -> lock.getProject(key) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewHeader(publish: (ModpackMsg) -> Unit, model: ModpackTabModel, values: ModpackFieldValues) {
    RecomposeProbe("OverviewHeader")
    val title = ModpackFieldSchema.fields.firstOrNull { it.hint.title }
    val badges = ModpackFieldSchema.fields.filter { it.hint.summary }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.commonPaddingSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        ) {
            if (title != null) {
                val value = values[title]
                InlineEditText(
                    value = value.text,
                    onCommit = { text ->
                        val next = title.textValue(text)
                        if (next?.text != value.text) publish(ModpackMsg.MetaWriteRequested(title.write(next)))
                    },
                    required = title.hint.required,
                    textStyle = JewelTheme.defaultTextStyle.copy(
                        fontSize = PakkuDesktopConstants.headerSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    editDescription = "Edit ${title.label}",
                ) {
                    Text(
                        value.text.ifBlank { title.hint.emptyText },
                        fontSize = PakkuDesktopConstants.headerSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                badges.forEach { field ->
                    values[field].summaryLabels.forEach { label ->
                        Badge(style = JewelTheme.badgeStyle.graySecondary) { Text(label) }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (model.overviewMode == OverviewMode.Json) {
                ChoiceToggle(
                    listOf("File" to !model.overviewShowDiff, "Diff" to model.overviewShowDiff),
                ) { index -> publish(ModpackMsg.OverviewDiffToggled(index == 1)) }
            }
            ChoiceToggle(
                listOf(
                    "Overview" to (model.overviewMode == OverviewMode.Form),
                    "JSON" to (model.overviewMode == OverviewMode.Json),
                ),
            ) { index ->
                publish(ModpackMsg.OverviewModeChanged(if (index == 0) OverviewMode.Form else OverviewMode.Json))
            }
        }
    }
}

@Composable
private fun ChoiceToggle(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    RecomposeProbe("OverviewChoiceToggle")
    SegmentedControl(
        options.mapIndexed { index, (label, selected) ->
            SegmentedControlButtonData(
                selected = selected,
                content = { _ -> Text(label) },
                onSelect = { onSelect(index) },
            )
        },
    )
}

@Composable
private fun OverviewIslands(
    publish: (ModpackMsg) -> Unit,
    model: ModpackTabModel,
    values: ModpackFieldValues,
    projects: List<Project>,
    projectOf: (String) -> Project?,
) {
    RecomposeProbe("OverviewIslands")
    var pickingOverrides by remember { mutableStateOf(false) }
    val unknown = model.configText?.unknownKeys.orEmpty().map { (name, _) -> ModpackFieldSchema.unknownField(name) }
    val islands = (ModpackFieldSchema.fields.filterNot { it.hint.title } + unknown)
        .groupBy { it.island }
        .toSortedMap()
        .toList()

    val listed = islands.filter { it.first.wide }
    val settings = islands.filter { !it.first.wide }
    if (listed.isEmpty() || settings.isEmpty()) {
        IslandColumn(islands, values, model, publish, projectOf) { pickingOverrides = true }
    } else {
        val splitState = remember { SplitLayoutState(0.58f) }
        val draggableWidth = 16.dp
        HorizontalSplitLayout(
            state = splitState,
            dividerStyle = animatedDividerStyle(),
            first = {
                Box(Modifier.fillMaxSize().padding(end = draggableWidth / 2)) {
                    IslandColumn(listed, values, model, publish, projectOf) { pickingOverrides = true }
                }
            },
            second = {
                Box(Modifier.fillMaxSize().padding(start = draggableWidth / 2)) {
                    IslandColumn(settings, values, model, publish, projectOf) { pickingOverrides = true }
                }
            },
            modifier = Modifier.fillMaxSize(),
            firstPaneMinWidth = 320.dp,
            secondPaneMinWidth = 300.dp,
            draggableWidth = draggableWidth,
        )
    }
    val overrides = islands.firstNotNullOfOrNull { (_, fields) -> fields.firstOrNull { it.hint.projectCard } }
    if (pickingOverrides && overrides != null) {
        AddOverrideDialog(
            projects = projects,
            already = (values[overrides] as? JsonObject)?.keys.orEmpty(),
            onDismiss = { pickingOverrides = false },
            onAdd = { keys ->
                val current = values[overrides] as? JsonObject ?: JsonObject(emptyMap())
                val ordered = linkedMapOf<String, JsonElement>()
                for (key in keys) {
                    if (key !in current) ordered[key] = JsonObject(emptyMap())
                }
                for ((key, value) in current) {
                    if (key !in ordered) ordered[key] = value
                }
                publish(ModpackMsg.MetaWriteRequested(overrides.write(JsonObject(ordered))))
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
                                    Text(project.name.values.firstOrNull() ?: key, fontWeight = FontWeight.Medium)
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

/** One pane of outline cards, scrolled like the Projects list. */
@Composable
private fun IslandColumn(
    islands: List<Pair<ModpackIsland, List<ModpackField>>>,
    values: ModpackFieldValues,
    model: ModpackTabModel,
    publish: (ModpackMsg) -> Unit,
    projectOf: (String) -> Project?,
    onPickOverrides: () -> Unit,
) {
    RecomposeProbe("OverviewIslandColumn")
    val scrollState = rememberScrollState()
    Row(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = PakkuDesktopConstants.commonPaddingSize, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.islandSpacing),
        ) {
            islands.forEach { (island, fields) ->
                val overrides = fields.firstOrNull { it.hint.projectCard }
                IslandCard(
                    title = island.title,
                    description = island.description,
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        if (overrides != null) {
                            FloatingActionIcon(
                                onClick = onPickOverrides,
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
                        }
                    },
                ) {
                    fields.forEach { field ->
                        ModpackFieldSelection(
                            field = field,
                            value = values[field],
                            errors = model.metaFieldErrors(field.key),
                            onEdit = { next -> field.write(next) },
                            publish = publish,
                            projectOf = projectOf,
                        )
                    }
                }
            }
        }
        VerticalScrollbar(
            scrollState,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun OverviewJson(model: ModpackTabModel) {
    RecomposeProbe("OverviewJson")
    if (model.overviewShowDiff) {
        DiffViewer(model.configDiff, Modifier.fillMaxSize())
        return
    }

    val text = model.configText?.text
    val scrollState = rememberScrollState()
    Row(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = PakkuDesktopConstants.commonPaddingSize, end = 8.dp),
        ) {
            if (text == null) {
                Text(
                    "${ConfigFile.FILE_NAME} could not be read.",
                    color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
                )
            } else {
                val shape = RoundedCornerShape(PakkuDesktopConstants.cardCornerRadius)
                SelectableText(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(PakkuDesktopConstants.cardBorderWidth, PakkuDesktopConstants.cardBorderColor, shape)
                        .padding(PakkuDesktopConstants.cardPadding),
                    fontFamily = FontFamily.Monospace,
                    fontSize = PakkuDesktopConstants.cardCaptionSize,
                )
            }
        }
        VerticalScrollbar(scrollState, modifier = Modifier.fillMaxHeight())
    }
}
