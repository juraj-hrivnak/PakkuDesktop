/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.OutlinedSlimButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProviderIcon
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.InlineEditText
import teksturepako.pakkuDesktop.pkui.component.IslandCard
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

private const val EmptyValue = "Not set"

/**
 * One field of the Overview form, chosen by its widget. [value] is the stored value; [onEdit]
 * turns a replacement into a write. Nested fields pass their own [value] and [onEdit].
 */
@Composable
fun ModpackFieldSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    onEdit: (JsonElement?) -> MetaWrite,
    publish: (ModpackMsg) -> Unit,
    projectOf: (String) -> Project? = { null },
) {
    RecomposeProbe("ModpackFieldSelection")
    val write: (JsonElement?) -> Unit = { next -> publish(ModpackMsg.MetaWriteRequested(onEdit(next))) }

    when (field.widget) {
        FieldWidget.Text, FieldWidget.Number -> TextSelection(field, value, write)
        FieldWidget.Multiline -> TextSelection(field, value, write, multiline = true)
        FieldWidget.Boolean -> BooleanSelection(field, value, write)
        FieldWidget.Choice -> ChoiceSelection(field, value, errors, write)
        FieldWidget.StringList -> StringListSelection(field, value, errors, write)
        FieldWidget.StringMap -> StringMapSelection(field, value, errors, write)
        FieldWidget.Object -> ObjectSelection(field, value, errors, onEdit, publish)
        FieldWidget.ObjectMap -> ObjectMapSelection(field, value, errors, onEdit, publish, projectOf)
        FieldWidget.RawJson -> RawJsonSelection(field, value, errors, write)
    }
}

// -- Text --

@Composable
private fun TextSelection(
    field: ModpackField,
    value: JsonElement,
    write: (JsonElement?) -> Unit,
    multiline: Boolean = false,
) {
    RecomposeProbe("ModpackTextSelection")
    Setting(field.label, field.hint.description) {
        InlineEditText(
            value = value.text,
            onCommit = { text ->
                val next = field.textValue(text)
                if (next?.text != value.text) write(next)
            },
            modifier = Modifier.fillMaxWidth(),
            multiline = multiline,
            required = field.hint.required,
            placeholder = field.hint.placeholder,
            editDescription = "Edit ${field.label}",
        ) {
            Text(
                value.text.ifBlank { field.hint.emptyText.ifBlank { field.hint.placeholder.ifBlank { EmptyValue } } },
                modifier = Modifier.weight(1f),
                color = if (value.text.isBlank()) secondary() else JewelTheme.contentColor,
            )
            if (field.resettable && value.text.isNotBlank()) {
                ResetButton { write(null) }
            }
        }
    }
}

// -- Boolean --

@Composable
private fun BooleanSelection(field: ModpackField, value: JsonElement, write: (JsonElement?) -> Unit) {
    RecomposeProbe("ModpackBooleanSelection")
    val overridden = value.isOverride()
    var selected by remember { mutableStateOf(value.text.toBoolean()) }
    LaunchedEffect(value.text) { selected = value.text.toBoolean() }

    if (field.nullable) {
        Setting(field.label, field.hint.description) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentedControl(
                    listOf(true, false).map { option ->
                        SegmentedControlButtonData(
                            selected = overridden && selected == option,
                            content = { _ -> Text(option.toString()) },
                            onSelect = {
                                selected = option
                                write(field.textValue(option.toString()))
                            },
                        )
                    },
                )
                if (overridden) ResetButton { write(null) }
            }
        }
        return
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        ) {
            Text(field.label, fontWeight = FontWeight.Medium)
            if (!field.hint.description.isNullOrBlank()) {
                Text(
                    field.hint.description,
                    color = secondary(),
                    fontSize = PakkuDesktopConstants.cardCaptionSize,
                )
            }
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { checked ->
                selected = checked
                write(field.textValue(checked.toString()))
            },
        )
    }
}

// -- Choice --

@Composable
private fun ChoiceSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackChoiceSelection")
    var selected by remember { mutableStateOf(value.text) }
    LaunchedEffect(value.text) { selected = value.text }
    // A rejected write leaves the file on the previous value — snap the buttons back with it.
    LaunchedEffect(errors) { if (errors.isNotEmpty()) selected = value.text }

    val options = field.options.let { known ->
        if (value.text.isNotBlank() && known.none { it.equals(value.text, ignoreCase = true) }) known + value.text else known
    }

    Setting(field.label, field.hint.description) {
        if (options.isEmpty()) {
            Text(EmptyValue, color = secondary())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentedControl(
                    options.map { option ->
                        SegmentedControlButtonData(
                            selected = selected.equals(option, ignoreCase = true),
                            content = { _ -> Text(field.hint.optionLabels[option] ?: option) },
                            onSelect = {
                                selected = option
                                if (!option.equals(value.text, ignoreCase = true)) write(field.textValue(option))
                            },
                        )
                    },
                )
                if (field.resettable && value.isOverride()) {
                    ResetButton { write(null) }
                }
            }
        }
        FieldErrors(errors)
    }
}

// -- String list --

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StringListSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackStringListSelection")
    val entries = value.strings

    Setting(field.label, field.hint.description) {
        if (entries.isEmpty()) {
            Text(EmptyValue, color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                entries.forEach { entry ->
                    ValueChip(entry, onRemove = { write(stringListValue(entries - entry)) })
                }
            }
        }
        AddRow(field.hint.placeholder) { text ->
            if (text !in entries) write(stringListValue(entries + text))
        }
        FieldErrors(errors)
    }
}

// -- String map --

@Composable
private fun StringMapSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackStringMapSelection")
    val entries = value.stringEntries
    val shown = if (field.hint.pinOptions) {
        field.options.map { option -> option to entries.firstOrNull { it.first == option }?.second.orEmpty() }
    } else {
        entries
    }
    val addable = field.options.filter { option -> entries.none { it.first == option } }

    Setting(field.label, field.hint.description) {
        if (shown.isEmpty()) {
            Text(EmptyValue, color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        }
        shown.forEach { (name, entryValue) ->
            MapEntry(
                label = field.hint.optionLabels[name] ?: name,
                value = entryValue,
                placeholder = field.hint.optionPlaceholders[name] ?: field.hint.placeholder,
                removable = !field.hint.pinOptions && entryValue.isNotBlank(),
                resettable = field.hint.pinOptions && entryValue.isNotBlank(),
            ) { next ->
                write(stringMapValue(mapEntries(entries, name, next)))
            }
        }
        if (!field.hint.pinOptions && addable.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add",
                    modifier = Modifier.width(PakkuDesktopConstants.settingLabelWidth),
                    color = secondary(),
                )
                SegmentedControl(
                    addable.map { name ->
                        SegmentedControlButtonData(
                            selected = false,
                            content = { _ -> Text(field.hint.optionLabels[name] ?: name) },
                            onSelect = { write(stringMapValue(entries + (name to ""))) },
                        )
                    },
                )
            }
        }
        FieldErrors(errors)
    }
}

/** Label column plus an inline-edited value, so every entry in a map lines up. */
@Composable
private fun MapEntry(
    label: String,
    value: String,
    placeholder: String,
    removable: Boolean,
    resettable: Boolean = false,
    onChange: (String?) -> Unit,
) {
    RecomposeProbe("ModpackMapEntry")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(PakkuDesktopConstants.settingLabelWidth),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        InlineEditText(
            value = value,
            onCommit = { text ->
                val next = text.ifEmpty { null }
                if (next != value.ifEmpty { null }) onChange(next)
            },
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            editDescription = "Edit $label",
        ) {
            Text(
                value.ifBlank { placeholder.ifBlank { EmptyValue } },
                modifier = Modifier.weight(1f),
                color = if (value.isBlank()) secondary() else JewelTheme.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (resettable) {
            ResetButton { onChange(null) }
        }
        if (removable) {
            RemoveButton("Remove $label") { onChange(null) }
        }
    }
}

// -- Object --

@Composable
private fun ObjectSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    onEdit: (JsonElement?) -> MetaWrite,
    publish: (ModpackMsg) -> Unit,
) {
    RecomposeProbe("ModpackObjectSelection")
    val obj = value as? JsonObject ?: JsonObject(emptyMap())
    if (field.fields.isEmpty()) {
        RawJsonSelection(field, value, errors) { publish(ModpackMsg.MetaWriteRequested(onEdit(it))) }
        return
    }
    Setting(field.label, field.hint.description) {
        if (field.nullable && !value.hasValues) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    field.hint.emptyText.ifBlank { EmptyValue },
                    modifier = Modifier.weight(1f),
                    color = secondary(),
                )
                OutlinedSlimButton(
                    onClick = { publish(ModpackMsg.MetaWriteRequested(onEdit(JsonObject(emptyMap())))) },
                ) { Text("Add") }
            }
        } else {
            field.fields.forEach { child ->
                ModpackFieldSelection(
                    field = child,
                    value = obj[child.serialName] ?: JsonNull,
                    errors = emptyList(),
                    onEdit = { next -> onEdit(value.withChild(child.serialName, next)) },
                    publish = publish,
                )
            }
        }
        FieldErrors(errors)
    }
}

// -- Object map --

@Composable
private fun ObjectMapSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    onEdit: (JsonElement?) -> MetaWrite,
    publish: (ModpackMsg) -> Unit,
    projectOf: (String) -> Project?,
) {
    RecomposeProbe("ModpackObjectMapSelection")
    val entries = (value as? JsonObject)?.toList().orEmpty()
    val onDisk = entries.map { it.first }.toSet()
    // Names added in this session that are not on disk until a property is set.
    var pending by remember { mutableStateOf(listOf<String>()) }
    var editing by remember { mutableStateOf(setOf<String>()) }
    SideEffect {
        if (pending.any { it in onDisk }) pending = pending.filter { it !in onDisk }
    }
    val rows = entries + pending.filter { it !in onDisk }.map { it to JsonObject(emptyMap()) }

    Setting(field.label, field.hint.description) {
        Column(verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing)) {
            AddRow(field.hint.placeholder) { name ->
                if (name in onDisk || name in pending) return@AddRow
                pending = pending + name
                editing = editing + name
            }
            if (rows.isEmpty()) {
                Text(
                    field.hint.emptyText.ifBlank { EmptyValue },
                    color = secondary(),
                    fontSize = PakkuDesktopConstants.cardCaptionSize,
                )
            }
            rows.forEach { (name, entry) ->
                val editingEntry = name in editing
                val children = if (editingEntry) {
                    field.fields
                } else {
                    field.fields.filter { child -> entry.child(child.serialName).isOverride() }
                }
                val project = if (field.hint.projectCard) projectOf(name) else null
                if (project != null) {
                    ProjectOverrideCard(
                        project = project,
                        editing = editingEntry,
                        onEditingChange = { on ->
                            editing = if (on) editing + name else editing - name
                            if (!on && name !in onDisk) pending = pending - name
                        },
                        onRemove = {
                            editing = editing - name
                            pending = pending - name
                            if (name in onDisk) {
                                publish(ModpackMsg.MetaWriteRequested(onEdit(value.withChild(name, null))))
                            }
                        },
                        showBody = children.isNotEmpty(),
                    ) {
                        children.forEach { child ->
                            OverrideChild(child, entry, name, value, editingEntry, onEdit, publish)
                        }
                    }
                } else {
                    IslandCard(
                        title = name,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            OverrideActions(
                                name = name,
                                editing = editingEntry,
                                onEditingChange = { on ->
                                    editing = if (on) editing + name else editing - name
                                    if (!on && name !in onDisk) pending = pending - name
                                },
                                onRemove = {
                                    editing = editing - name
                                    pending = pending - name
                                    if (name in onDisk) {
                                        publish(ModpackMsg.MetaWriteRequested(onEdit(value.withChild(name, null))))
                                    }
                                },
                            )
                        },
                    ) {
                        if (children.isEmpty()) {
                            Text(
                                "No overrides",
                                color = secondary(),
                                fontSize = PakkuDesktopConstants.cardCaptionSize,
                            )
                        }
                        children.forEach { child ->
                            OverrideChild(child, entry, name, value, editingEntry, onEdit, publish)
                        }
                    }
                }
            }
            FieldErrors(errors)
        }
    }
}

/**
 * An overview card for one project override. Same information as a Projects-tab card — name, type,
 * providers — laid out in the overview card chrome, not that card itself.
 */
@Composable
private fun ProjectOverrideCard(
    project: Project,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    showBody: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    RecomposeProbe("ProjectOverrideCard")
    val shape = RoundedCornerShape(PakkuDesktopConstants.cardCornerRadius)
    val displayName = project.name.values.firstOrNull() ?: project.pakkuId.orEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.islandTintAlpha))
            .border(PakkuDesktopConstants.cardBorderWidth, PakkuDesktopConstants.cardBorderColor, shape)
            .padding(PakkuDesktopConstants.cardPadding),
        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                Text(
                    displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = PakkuDesktopConstants.cardTitleSize,
                )
                project.getProviders().groupBy { provider -> project.slug[provider.serialName] }.forEach { (slug, providers) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        providers.forEach { provider -> ProviderIcon(provider) }
                        slug?.let {
                            Text(
                                it,
                                color = secondary(),
                                fontSize = PakkuDesktopConstants.cardCaptionSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                Text(
                    project.type.prettyName,
                    color = secondary(),
                    fontSize = PakkuDesktopConstants.cardCaptionSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OverrideActions(displayName, editing, onEditingChange, onRemove)
            }
        }
        if (showBody) {
            Divider(Orientation.Horizontal)
            content()
        }
    }
}

@Composable
private fun OverrideActions(
    name: String,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Edit", color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        Switch(checked = editing, onCheckedChange = onEditingChange)
        RemoveButton("Remove $name", onRemove)
    }
}

@Composable
private fun OverrideChild(
    child: ModpackField,
    entry: JsonElement,
    name: String,
    map: JsonElement,
    editing: Boolean,
    onEdit: (JsonElement?) -> MetaWrite,
    publish: (ModpackMsg) -> Unit,
) {
    val childValue = entry.child(child.serialName)
    if (editing) {
        ModpackFieldSelection(
            field = child,
            value = childValue,
            errors = emptyList(),
            onEdit = { next -> onEdit(map.withChild(name, entry.withChild(child.serialName, next))) },
            publish = publish,
        )
    } else {
        OverrideValue(child, childValue)
    }
}

// -- Raw JSON --

@Composable
private fun RawJsonSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackRawJsonSelection")
    val pretty = remember(value) { value.toPrettyJson() }
    val state = remember(pretty) { TextFieldState(pretty, TextRange(0, pretty.length)) }
    var parseError by remember(pretty) { mutableStateOf<ActionError?>(null) }

    fun commit() {
        val text = state.text.toString().trim()
        if (text == pretty) return
        parseJsonValue(text)
            .onOk { parsed ->
                parseError = null
                write(parsed)
            }
            .onErr { parseError = it }
    }

    Setting(field.label, field.hint.description) {
        TextArea(
            state,
            Modifier
                .fillMaxWidth()
                .height(PakkuDesktopConstants.multilineInputHeight)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            commit()
                            true
                        }
                        else -> false
                    }
                },
            textStyle = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace),
        )
        FieldErrors(listOfNotNull(parseError) + errors)
    }
}

// -- Chrome --

/** A stored property, as opposed to null or blank. `false` and `0` count. */
private fun JsonElement.isOverride(): Boolean = when (this) {
    is JsonNull -> false
    is JsonArray -> any { it.isOverride() }
    is JsonObject -> values.any { it.isOverride() }
    is JsonPrimitive -> !isString || content.isNotBlank()
}

private fun JsonElement.child(name: String): JsonElement = (this as? JsonObject)?.get(name) ?: JsonNull

/** Read-only label and value for one overridden property. */
@Composable
private fun OverrideValue(field: ModpackField, value: JsonElement) {
    RecomposeProbe("ModpackOverrideValue")
    val text = when (value) {
        is JsonArray -> value.strings.joinToString(", ")
        is JsonObject -> value.stringEntries.joinToString(", ") { (key, entry) ->
            if (entry.isBlank()) key else "$key $entry"
        }
        else -> field.hint.optionLabels[value.text] ?: value.text
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            field.label,
            modifier = Modifier.width(PakkuDesktopConstants.settingLabelWidth),
            color = secondary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text.ifBlank { EmptyValue }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** [name] replaced, or removed when [value] is null. */
private fun mapEntries(entries: List<Pair<String, String>>, name: String, value: String?): List<Pair<String, String>> =
    if (value == null) entries.filter { it.first != name }
    else if (entries.any { it.first == name }) entries.map { if (it.first == name) name to value else it }
    else entries + (name to value)

/** Label, optional description, then the control. Every field in a card shares this stack. */
@Composable
private fun Setting(label: String, description: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        if (!description.isNullOrBlank()) {
            Text(description, color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        }
        content()
    }
}

@Composable
private fun secondary() = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha)

@Composable
private fun AddRow(placeholder: String, onAdd: (String) -> Unit) {
    RecomposeProbe("ModpackAddRow")
    val input = rememberTextFieldState()

    fun submit() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        onAdd(text)
        input.clearText()
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            input,
            Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            submit()
                            true
                        }
                        else -> false
                    }
                },
            placeholder = { Text(placeholder) },
        )
        PkUiTooltip({ Text("Add") }) {
            IconButton(
                onClick = ::submit,
                enabled = input.text.isNotBlank(),
                modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
            ) {
                Icon(AllIconsKeys.General.Add, "Add", tint = JewelTheme.contentColor)
            }
        }
    }
}

@Composable
private fun ValueChip(text: String, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(PakkuDesktopConstants.cardCornerRadius / 2)
    Row(
        Modifier
            .clip(shape)
            .background(JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.chipBackgroundAlpha))
            .padding(horizontal = PakkuDesktopConstants.cardContentSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
    ) {
        Text(text, fontSize = PakkuDesktopConstants.cardCaptionSize)
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
        ) {
            Icon(AllIconsKeys.Actions.Close, "Remove $text", tint = secondary())
        }
    }
}

@Composable
private fun RemoveButton(description: String, onClick: () -> Unit) {
    PkUiTooltip({ Text(description) }) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
        ) {
            Icon(AllIconsKeys.General.Delete, description, tint = JewelTheme.contentColor)
        }
    }
}

@Composable
private fun ResetButton(onClick: () -> Unit) {
    PkUiTooltip({ Text("Reset to default") }) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
        ) {
            Icon(PakkuDesktopIcons.rollback, "reset", tint = JewelTheme.contentColor)
        }
    }
}

@Composable
private fun FieldErrors(errors: List<ActionError>) {
    if (errors.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing)) {
        errors.forEach { ActionErrorContent(it) }
    }
}
