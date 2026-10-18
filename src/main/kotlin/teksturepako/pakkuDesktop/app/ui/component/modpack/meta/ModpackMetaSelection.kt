/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

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
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.OutlinedSlimButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.menuStyle
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectPropertyEditor
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectPropertyReadOnly
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProviderIcon
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropertyDisplay
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.InlineEditText
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown
import teksturepako.pakkuDesktop.pkui.component.pkUiSubmenu
import teksturepako.pakkuDesktop.pkui.component.IslandCard
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableListItemScope

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
    onEdit: (JsonElement?) -> MetaWrite?,
    publish: (ModpackMsg) -> Unit,
    projectOf: (String) -> Project? = { null },
) {
    RecomposeProbe("ModpackFieldSelection")
    val write: (JsonElement?) -> Unit = { next ->
        onEdit(next)?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
    }

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
                PkUiDropdown(menuContent = {
                    options.forEach { option ->
                        selectableItem(
                            selected = selected.equals(option, ignoreCase = true),
                            onClick = {
                                selected = option
                                if (!option.equals(value.text, ignoreCase = true)) write(field.textValue(option))
                            },
                        ) { MenuLabel(field.hint.optionLabels[option] ?: option) }
                    }
                }) {
                    Text(
                        selected.ifBlank { EmptyValue },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (field.resettable && value.isOverride()) {
                    ResetButton { write(null) }
                }
            }
        }
        FieldErrors(errors)
    }
}

// -- String list --

@Composable
private fun StringListSelection(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackStringListSelection")
    val entries = value.strings
    var local by remember { mutableStateOf<List<String>?>(null) }
    val shown = local ?: entries
    SideEffect { if (local != null && local == entries) local = null }
    LaunchedEffect(errors) { if (errors.isNotEmpty()) local = null }
    fun publishList(next: List<String>) {
        local = next
        write(stringListValue(next))
    }
    var editing by remember { mutableStateOf<String?>(null) }

    Setting(field.label, field.hint.description) {
        if (shown.isEmpty()) {
            Text(EmptyValue, color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        } else {
            val order = remember(shown) { shown.toList() }
            ReorderableColumn(
                list = order,
                onSettle = { from, to ->
                    if (from != to) publishList(order.toMutableList().apply { add(to, removeAt(from)) })
                },
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) { _, entry, _ ->
                key(entry) {
                    ReorderableItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DragHandle()
                            if (editing == entry) {
                                MetaChipVersionField(
                                    version = entry,
                                    placeholder = field.hint.placeholder,
                                    onCommit = { text ->
                                        editing = null
                                        val next = text.trim()
                                        if (next.isEmpty()) publishList(order - entry)
                                        else if (next != entry && next !in order) {
                                            publishList(order.map { if (it == entry) next else it })
                                        }
                                    },
                                )
                            } else {
                                MetaMenuChip(entry) {
                                    selectableItem(
                                        selected = false,
                                        iconKey = AllIconsKeys.Actions.Edit,
                                        onClick = { editing = entry },
                                    ) { MenuLabel("Edit") }
                                    selectableItem(
                                        selected = false,
                                        iconKey = AllIconsKeys.General.Delete,
                                        onClick = { publishList(order - entry) },
                                    ) { MenuLabel("Remove") }
                                }
                            }
                        }
                    }
                }
            }
        }
        MetaAddRow(field.hint.placeholder) { text ->
            if (text !in shown) publishList(shown + text)
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
        if (field.hint.chips) {
            ChipMap(field, entries, addable, errors, write)
            FieldErrors(errors)
            return@Setting
        }
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

/** Loader-style pills: `Forge 47.3.0` with a chevron, and a dashed `+ Add`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipMap(
    field: ModpackField,
    entries: List<Pair<String, String>>,
    addable: List<String>,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ModpackChipMap")
    var local by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    val shown = local ?: entries
    SideEffect { if (local != null && local == entries) local = null }
    LaunchedEffect(errors) { if (errors.isNotEmpty()) local = null }
    var versionEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun publishMap(next: List<Pair<String, String>>) {
        local = next
        write(stringMapValue(next))
    }
    fun add(name: String) {
        if (shown.any { it.first == name }) return
        publishMap(shown + (name to ""))
        versionEdit = name to ""
    }
    val order = remember(shown) { shown.toList() }
    if (order.isNotEmpty()) {
        ReorderableColumn(
            list = order,
            onSettle = { from, to ->
                if (from != to) publishMap(order.toMutableList().apply { add(to, removeAt(from)) })
            },
            verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        ) { _, entry, _ ->
            val (name, version) = entry
            key(name) {
                ReorderableItem {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DragHandle()
                        MetaMenuChip(chipLabel(field, name, version)) {
                    if (field.options.isNotEmpty()) {
                        pkUiSubmenu(
                            iconKey = AllIconsKeys.General.Settings,
                            submenu = {
                                field.options.forEach { option ->
                                    val taken = option != name && shown.any { it.first == option }
                                    selectableItem(
                                        selected = option == name,
                                        enabled = !taken,
                                        onClick = {
                                            if (option != name) {
                                                publishMap(order.map { item ->
                                                    if (item.first == name) option to version else item
                                                })
                                            }
                                        },
                                    ) {
                                        MenuLabel(field.hint.optionLabels[option] ?: option)
                                    }
                                }
                            },
                        ) { MenuLabel("Change loader") }
                    }
                    selectableItem(
                        selected = false,
                        iconKey = AllIconsKeys.Actions.Edit,
                        onClick = { versionEdit = name to version },
                    ) { MenuLabel("Change version") }
                    selectableItem(
                        selected = false,
                        iconKey = AllIconsKeys.General.Delete,
                        onClick = { publishMap(mapEntries(order, name, null)) },
                    ) { MenuLabel("Remove") }
                        }
                    }
                }
            }
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
    ) {
        val remaining = addable.filter { option -> order.none { it.first == option } }
        if (remaining.isNotEmpty()) {
            AddMenuChip(remaining.map { field.hint.optionLabels[it] ?: it }) { index -> add(remaining[index]) }
        }
        CustomLoaderField(taken = order.map { it.first }.toSet(), onAdd = ::add)
    }
    val editingVersion = versionEdit
    if (editingVersion != null) {
        LoaderVersionDialog(
            name = editingVersion.first,
            version = editingVersion.second,
            onDismiss = { versionEdit = null },
            onSave = { text ->
                val (name, version) = editingVersion
                versionEdit = null
                val next = text.trim().ifBlank { null }
                if (next != version.ifBlank { null }) publishMap(mapEntries(shown, name, next))
            },
        )
    }
}

private fun chipLabel(field: ModpackField, name: String, version: String): String {
    val title = field.hint.optionLabels[name] ?: name
    return if (version.isBlank()) title else "$title $version"
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text,
        color = JewelTheme.menuStyle.colors.itemColors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ReorderableListItemScope.DragHandle() {
    Icon(
        AllIconsKeys.General.Drag,
        contentDescription = "Reorder",
        tint = secondary(),
        modifier = Modifier.size(16.dp).draggableHandle(),
    )
}

@Composable
private fun AddMenuChip(labels: List<String>, onAdd: (Int) -> Unit) {
    RecomposeProbe("ModpackAddMenuChip")
    PkUiDropdown(menuContent = {
        labels.forEachIndexed { index, label ->
            selectableItem(selected = false, iconKey = AllIconsKeys.General.Add, onClick = { onAdd(index) }) {
                MenuLabel(label)
            }
        }
    }) {
        Text("+ Add", color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
    }
}

/** Free loader name. Pakku stores loader names in lowercase and does not limit them to a fixed list. */
@Composable
private fun CustomLoaderField(taken: Set<String>, onAdd: (String) -> Unit) {
    RecomposeProbe("ModpackCustomLoaderField")
    val state = rememberTextFieldState()
    var armed by remember { mutableStateOf(false) }
    fun commit() {
        if (!armed) return
        val name = state.text.toString().trim().lowercase()
        if (name.isEmpty() || name in taken) return
        armed = false
        state.clearText()
        onAdd(name)
    }
    TextField(
        state,
        Modifier
            .width(PakkuDesktopConstants.settingLabelWidth)
            .onFocusChanged { focus ->
                if (focus.isFocused) armed = true else commit()
            }
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
        placeholder = { Text("Custom loader") },
    )
}

@Composable
private fun LoaderVersionDialog(
    name: String,
    version: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    RecomposeProbe("LoaderVersionDialog")
    val state = rememberTextFieldState(version)
    val focus = remember { FocusRequester() }
    fun save() = onSave(state.text.toString())
    PkUiDialog(
        onDismiss = onDismiss,
        title = "Loader version",
        subtitle = name,
        size = PkUiDialogSize.Small,
        initialFocus = focus,
        onConfirm = ::save,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            DefaultButton(onClick = ::save) { Text("Save") }
        },
    ) {
        TextField(
            state,
            Modifier.fillMaxWidth().focusRequester(focus),
            placeholder = { Text("Version") },
        )
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
    onEdit: (JsonElement?) -> MetaWrite?,
    publish: (ModpackMsg) -> Unit,
) {
    RecomposeProbe("ModpackObjectSelection")
    val obj = value as? JsonObject ?: JsonObject(emptyMap())
    if (field.fields.isEmpty()) {
        RawJsonSelection(field, value, errors) { next ->
            onEdit(next)?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
        }
        return
    }
    val drafting = field.nullable && !value.hasValues
    var draft by remember(value) { mutableStateOf(obj) }
    val shown = if (drafting) draft else obj
    Setting(field.label, field.hint.description) {
        if (field.nullable && field.resettable && value.hasValues) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ResetButton { onEdit(null)?.let { publish(ModpackMsg.MetaWriteRequested(it)) } }
            }
        }
        field.fields.forEach { child ->
            ModpackFieldSelection(
                field = child,
                value = shown[child.serialName] ?: JsonNull,
                errors = emptyList(),
                onEdit = { next ->
                    val updated = shown.withChild(child.serialName, next)
                    if (drafting) draft = updated
                    val ready = field.fields.none { required ->
                        !required.nullable && !updated.child(required.serialName).isOverride()
                    }
                    if (ready) onEdit(if (drafting) updated else value.withChild(child.serialName, next)) else null
                },
                publish = publish,
            )
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
    onEdit: (JsonElement?) -> MetaWrite?,
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

    fun renameOverride(from: String, to: String) {
        val next = to.trim()
        if (next.isEmpty() || next == from || rows.any { it.first == next }) return
        if (from in editing) editing = editing - from + next
        if (from !in onDisk) {
            pending = pending.map { if (it == from) next else it }
            return
        }
        val obj = value as? JsonObject ?: return
        val renamed = JsonObject(
            obj.entries.associate { (key, element) -> (if (key == from) next else key) to element },
        )
        onEdit(renamed)?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
    }

    Setting(field.label, field.hint.description) {
        Column(verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing)) {
            if (!field.hint.projectCard) {
                MetaAddRow(field.hint.placeholder) { name ->
                    if (name in onDisk || name in pending) return@MetaAddRow
                    pending = pending + name
                    editing = editing + name
                }
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
                if (field.hint.projectCard) {
                    ProjectOverrideCard(
                        entryKey = name,
                        project = project,
                        editing = editingEntry,
                        onRename = { next -> renameOverride(name, next) },
                        onEditingChange = { on ->
                            editing = if (on) editing + name else editing - name
                            if (!on && name !in onDisk) pending = pending - name
                        },
                        onRemove = {
                            editing = editing - name
                            pending = pending - name
                            if (name in onDisk) {
                                onEdit(value.withChild(name, null))?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
                            }
                        },
                        showBody = children.isNotEmpty(),
                    ) {
                        children.forEach { child ->
                            OverrideChild(child, entry, name, value, editingEntry, projectStyle = true, onEdit, publish)
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
                                        onEdit(value.withChild(name, null))?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
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
                            OverrideChild(child, entry, name, value, editingEntry, projectStyle = false, onEdit, publish)
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
    entryKey: String,
    project: Project?,
    editing: Boolean,
    onRename: (String) -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    showBody: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    RecomposeProbe("ProjectOverrideCard")
    val shape = RoundedCornerShape(PakkuDesktopConstants.cardCornerRadius)
    val displayName = project?.name?.values?.firstOrNull() ?: project?.pakkuId ?: entryKey
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
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
                InlineEditText(
                    value = entryKey,
                    onCommit = onRename,
                    required = true,
                    textStyle = JewelTheme.defaultTextStyle.copy(
                        fontSize = PakkuDesktopConstants.cardTitleSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    editDescription = "Edit override entry",
                ) {
                    Text(
                        entryKey,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = PakkuDesktopConstants.cardTitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (project == null) {
                    Text(
                        "Not a project in this modpack",
                        color = secondary(),
                        fontSize = PakkuDesktopConstants.cardCaptionSize,
                    )
                } else if (displayName != entryKey) {
                    Text(
                        displayName,
                        color = secondary(),
                        fontSize = PakkuDesktopConstants.cardCaptionSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                project?.getProviders()?.groupBy { provider -> project.slug[provider.serialName] }?.forEach { (slug, providers) ->
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
                if (project != null) {
                    Text(
                        project.type.prettyName,
                        color = secondary(),
                        fontSize = PakkuDesktopConstants.cardCaptionSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
        PkUiTooltip({ Text("Remove this override. $name stays in the modpack.") }) {
            OutlinedSlimButton(onClick = onRemove) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        PakkuDesktopIcons.rollback,
                        contentDescription = null,
                        tint = JewelTheme.contentColor,
                        modifier = Modifier.size(PakkuDesktopConstants.cardContentSpacing + PakkuDesktopConstants.controlSpacing),
                    )
                    Text("Clear override")
                }
            }
        }
    }
}

@Composable
private fun OverrideChild(
    child: ModpackField,
    entry: JsonElement,
    name: String,
    map: JsonElement,
    editing: Boolean,
    projectStyle: Boolean,
    onEdit: (JsonElement?) -> MetaWrite?,
    publish: (ModpackMsg) -> Unit,
) {
    val childValue = entry.child(child.serialName)
    val write: (JsonElement?) -> Unit = { next ->
        onEdit(map.withChild(name, entry.withChild(child.serialName, next)))
            ?.let { publish(ModpackMsg.MetaWriteRequested(it)) }
    }
    val listed = projectStyle && child.widget in setOf(
        FieldWidget.Choice,
        FieldWidget.Boolean,
        FieldWidget.Text,
        FieldWidget.StringList,
    )
    if (editing && listed) {
        ProjectPropertyEditor(child, childValue, write)
    } else if (editing) {
        ModpackFieldSelection(
            field = child,
            value = childValue,
            errors = emptyList(),
            onEdit = { next -> onEdit(map.withChild(name, entry.withChild(child.serialName, next))) },
            publish = publish,
        )
    } else if (projectStyle) {
        ProjectPropertyReadOnly(child, childValue)
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

/** Read-only label and value for one overridden property. */
@Composable
private fun OverrideValue(field: ModpackField, value: JsonElement) {
    RecomposeProbe("ModpackOverrideValue")
    val text = projectPropertyDisplay(field, value)
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
        if (label.isNotBlank()) Text(label, fontWeight = FontWeight.Medium)
        if (!description.isNullOrBlank()) {
            Text(description, color = secondary(), fontSize = PakkuDesktopConstants.cardCaptionSize)
        }
        content()
    }
}

@Composable
private fun secondary() = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha)

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
