/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.MetaMenuLabel
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.FieldWidget
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.MetaAddRow
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.MetaChipVersionField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.MetaMenuChip
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.stringListValue
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.text
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.strings
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.stringEntries
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.textValue
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

private const val EmptyValue = "Not set"

/** Properties of one `pakku.json` project override. */
val modpackProjectConfigFields: List<ModpackField>
    get() = teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema.fields
        .first { it.serialName == "projects" }
        .fields

fun projectPropertyDisplay(field: ModpackField, value: JsonElement): String = when (value) {
    is JsonArray -> value.strings.joinToString(", ")
    is JsonObject -> value.stringEntries.joinToString(", ") { (key, entry) ->
        if (entry.isBlank()) key else "$key $entry"
    }
    else -> field.hint.optionLabels[value.text] ?: value.text
}

@Composable
fun ProjectPropertyContentBox(
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.globalColors.borders.disabled,
    content: @Composable BoxScope.() -> Unit,
) {
    val shadow = Color.Black.copy(alpha = 0.06f)
    ContentBox(
        modifier = modifier,
        color = color,
        elevation = 2.dp,
        ambientShadowColor = shadow,
        spotShadowColor = shadow,
    ) {
        content()
    }
}

@Composable
fun ProjectPropertyEditor(
    field: ModpackField,
    value: JsonElement,
    write: (JsonElement?) -> Unit,
) {
    when (field.widget) {
        FieldWidget.Choice -> ProjectPropertyChoices(field, value, field.options, write) { field.textValue(it) }
        FieldWidget.Boolean -> ProjectPropertyChoices(field, value, listOf("true", "false"), write) {
            JsonPrimitive(it.toBoolean())
        }
        FieldWidget.Text -> ProjectPropertyText(field, value, write)
        FieldWidget.StringList -> ProjectPropertyStringList(field, value, write)
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectPropertyReadOnly(
    field: ModpackField,
    value: JsonElement,
) {
    RecomposeProbe("ProjectPropertyReadOnly")
    FlowRow(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
        ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(field.label) }
        when (field.widget) {
            FieldWidget.StringList -> {
                val items = value.strings.filter { it.isNotBlank() }
                if (items.isEmpty()) {
                    ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(EmptyValue) }
                } else {
                    items.forEach { item ->
                        ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(item) }
                    }
                }
            }
            else -> {
                val text = projectPropertyDisplay(field, value)
                ProjectPropertyContentBox(Modifier.padding(2.dp)) {
                    Text(text.ifBlank { EmptyValue })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectPropertyChoices(
    field: ModpackField,
    value: JsonElement,
    options: List<String>,
    write: (JsonElement?) -> Unit,
    encode: (String) -> JsonElement?,
) {
    RecomposeProbe("ProjectPropertyChoices")
    val current = value.text
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(field.label) } }
                if (field.resettable) {
                    Row {
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = { write(null) },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback,
                                    "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            FlowRow(Modifier.padding(2.dp)) {
                options.forEach { option ->
                    SegmentedControl(
                        listOf(
                            SegmentedControlButtonData(
                                selected = current.equals(option, ignoreCase = true),
                                content = { _ -> Text(field.hint.optionLabels[option] ?: option) },
                                onSelect = {
                                    if (!current.equals(option, ignoreCase = true)) write(encode(option))
                                },
                            ),
                        ),
                        Modifier.padding(2.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectPropertyText(
    field: ModpackField,
    value: JsonElement,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ProjectPropertyText")
    val state = rememberTextFieldState(value.text)
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(state.text) {
        val text = state.text.toString()
        if (!seeded) {
            seeded = true
            return@LaunchedEffect
        }
        val next = text.trim()
        if (next == value.text) return@LaunchedEffect
        write(if (next.isEmpty()) null else field.textValue(next))
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(field.label) } }
                if (field.resettable) {
                    Row {
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    state.clearText()
                                    write(null)
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback,
                                    "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            FlowRow(Modifier.padding(4.dp)) {
                TextField(state, placeholder = { Text(field.hint.placeholder) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectPropertyStringList(
    field: ModpackField,
    value: JsonElement,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ProjectPropertyStringList")
    val entries = value.strings
    var local by remember { mutableStateOf<List<String>?>(null) }
    val shown = local ?: entries
    SideEffect { if (local != null && local == entries) local = null }
    var editing by remember { mutableStateOf<String?>(null) }

    fun publishList(next: List<String>) {
        local = next
        write(if (next.isEmpty()) null else stringListValue(next))
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { ProjectPropertyContentBox(Modifier.padding(2.dp)) { Text(field.label) } }
                if (field.resettable) {
                    Row {
                        PkUiTooltip({ Text("Reset to default") }) {
                            IconButton(
                                onClick = {
                                    editing = null
                                    publishList(emptyList())
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                            ) {
                                Icon(
                                    PakkuDesktopIcons.rollback,
                                    "reset",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            Column(
                Modifier.padding(2.dp).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                if (shown.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                    ) {
                        shown.forEach { alias ->
                            key(alias) {
                                if (editing == alias) {
                                    MetaChipVersionField(
                                        version = alias,
                                        placeholder = field.hint.placeholder,
                                        onCommit = { text ->
                                            editing = null
                                            val next = text.trim()
                                            if (next.isEmpty()) publishList(shown - alias)
                                            else if (next != alias && next !in shown) {
                                                publishList(shown.map { if (it == alias) next else it })
                                            }
                                        },
                                    )
                                } else {
                                    MetaMenuChip(alias) {
                                        selectableItem(
                                            selected = false,
                                            iconKey = AllIconsKeys.Actions.Edit,
                                            onClick = { editing = alias },
                                        ) { MetaMenuLabel("Edit") }
                                        selectableItem(
                                            selected = false,
                                            iconKey = AllIconsKeys.General.Delete,
                                            onClick = { publishList(shown - alias) },
                                        ) { MetaMenuLabel("Remove") }
                                    }
                                }
                            }
                        }
                    }
                }
                MetaAddRow(field.hint.placeholder) { text ->
                    if (text !in shown) publishList(shown + text)
                }
            }
        }
    }
}
