/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.git.diff.DiffViewer
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.LineDiff
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
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
import teksturepako.pakkuDesktop.pkui.component.InlineEditText
import teksturepako.pakkuDesktop.pkui.component.IslandCard

@Composable
fun ModpackTab(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    RecomposeProbe("ModpackTab")
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

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.islandSpacing),
        ) {
            OverviewHeader(publish, model, values)
            if (model.overviewMode == OverviewMode.Json) {
                OverviewJson(publish, model)
            } else {
                OverviewIslands(publish, model, values) { key -> lock.getProject(key) }
            }
        }
        VerticalScrollbar(
            scrollState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun OverviewHeader(publish: (ModpackMsg) -> Unit, model: ModpackTabModel, values: ModpackFieldValues) {
    RecomposeProbe("OverviewHeader")
    val title = ModpackFieldSchema.fields.firstOrNull { it.hint.title }
    val subtitle = ModpackFieldSchema.fields
        .filter { it.hint.summary }
        .flatMap { values[it].summaryLabels }
        .joinToString("  ·  ")

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
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
                    fontSize = PakkuDesktopConstants.cardCaptionSize,
                )
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
                    "Form" to (model.overviewMode == OverviewMode.Form),
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
    projectOf: (String) -> Project?,
) {
    RecomposeProbe("OverviewIslands")
    val unknown = model.configText?.unknownKeys.orEmpty().map { (name, _) -> ModpackFieldSchema.unknownField(name) }
    val byIsland = (ModpackFieldSchema.fields.filterNot { it.hint.title } + unknown).groupBy { it.island }

    Column(verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.islandSpacing)) {
        byIsland.toSortedMap().forEach { (island, fields) ->
            IslandCard(
                title = island.title,
                description = island.description,
                modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun OverviewJson(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    RecomposeProbe("OverviewJson")
    val text = model.configText?.text
    val diff = remember(model.overviewShowDiff, text, model.configBaselineText) {
        if (!model.overviewShowDiff || text == null) null
        else LineDiff.unified(model.configBaselineText.orEmpty(), text, ConfigFile.FILE_NAME)
    }

    if (text == null) {
        Text(
            "${ConfigFile.FILE_NAME} could not be read.",
            color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
        )
        return
    }
    if (diff != null) {
        if (diff.hunks.isEmpty()) {
            Text(
                "No changes this session.",
                color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
            )
        } else {
            DiffViewer(diff, Modifier.fillMaxWidth().height(PakkuDesktopConstants.jsonDiffHeight))
        }
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
