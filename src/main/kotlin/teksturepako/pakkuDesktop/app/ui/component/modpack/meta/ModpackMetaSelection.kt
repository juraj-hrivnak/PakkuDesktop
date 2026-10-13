/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarActionButton
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
import teksturepako.pakkuDesktop.pkui.component.rememberListComboBoxState
import kotlinx.coroutines.delay

private val FieldVerticalPadding = 6.dp
private const val ModpackAutoSaveDebounceMs = 450L
private val LoaderNameWidth = 96.dp

/** Single-line modpack inputs (text fields, combos, hero name in edit mode). */
internal val ModpackSingleLineInputHeight = 32.dp

/** Description editor / read-only block height. */
internal val ModpackDescriptionInputHeight = 120.dp

@Composable
fun ModpackFieldLabel(label: String) {
    Text(
        label,
        color = JewelTheme.contentColor.copy(alpha = 0.65f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun ModpackFieldHint(text: String) {
    Text(
        text,
        color = JewelTheme.contentColor.copy(alpha = 0.45f),
        fontSize = 12.sp,
    )
}

@Composable
private fun MutedValue(text: String) {
    Text(text, color = JewelTheme.contentColor.copy(alpha = 0.4f), fontSize = 13.sp)
}

/**
 * Pack metadata string field. Commits on focus loss or Enter — not on every keystroke.
 */
@Composable
fun ModpackCommitTextField(
    label: String,
    value: String,
    placeholder: String = "—",
    singleLine: Boolean = true,
    publish: (ModpackMsg) -> Unit,
    model: ModpackTabModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = FieldVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label.isNotBlank()) ModpackFieldLabel(label)

        if (model.editingModpack) {
            if (singleLine) {
                CommitTextField(
                    value = value,
                    placeholder = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    onCommit = { text ->
                        publish(
                            ModpackMsg.MetaWriteRequested(
                                MetaWrite { config, lock -> mutate(config, lock, text) },
                            ),
                        )
                    },
                )
            } else {
                CommitTextArea(
                    value = value,
                    placeholder = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    onCommit = { text ->
                        publish(
                            ModpackMsg.MetaWriteRequested(
                                MetaWrite { config, lock -> mutate(config, lock, text) },
                            ),
                        )
                    },
                )
            }
        } else if (value.isBlank()) {
            MutedValue(placeholder)
        } else if (!singleLine) {
            ModpackDescriptionPreview(value)
        } else {
            Text(value, color = JewelTheme.contentColor, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Writes [pakku.json] / lock changes while Edit is on — debounced, plus flush on blur or field dispose. */
@Composable
internal fun LaunchModpackAutoSave(
    value: String,
    textFieldState: TextFieldState,
    onCommit: (String) -> Unit,
) {
    LaunchedEffect(textFieldState, value) {
        snapshotFlow { textFieldState.text.toString() }.collect { latest ->
            delay(ModpackAutoSaveDebounceMs)
            if (textFieldState.text.toString() != latest) return@collect
            if (latest != value) onCommit(latest)
        }
    }
    DisposableEffect(value) {
        onDispose {
            val text = textFieldState.text.toString()
            if (text != value) onCommit(text)
        }
    }
}

/** Single-line field: commits on focus loss or Enter when the text changed. */
@Composable
private fun CommitTextField(
    value: String,
    placeholder: String,
    modifier: Modifier,
    onCommit: (String) -> Unit,
) {
    val textFieldState = rememberTextFieldState(value)
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!focused && textFieldState.text.toString() != value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    fun commit() {
        val text = textFieldState.text.toString()
        if (text != value) onCommit(text)
    }

    LaunchModpackAutoSave(value, textFieldState, onCommit)

    TextField(
        textFieldState,
        modifier
            .height(ModpackSingleLineInputHeight)
            .onFocusChanged { focus ->
                val wasFocused = focused
                focused = focus.isFocused
                if (wasFocused && !focus.isFocused) commit()
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
        placeholder = { Text(placeholder) },
    )
}

/** Multi-line description block: auto-saves while typing; Enter inserts a newline. */
@Composable
private fun CommitTextArea(
    value: String,
    placeholder: String,
    modifier: Modifier,
    onCommit: (String) -> Unit,
) {
    val textFieldState = rememberTextFieldState(value)
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!focused && textFieldState.text.toString() != value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    fun commit() {
        val text = textFieldState.text.toString()
        if (text != value) onCommit(text)
    }

    LaunchModpackAutoSave(value, textFieldState, onCommit)

    TextArea(
        textFieldState,
        modifier
            .height(ModpackDescriptionInputHeight)
            .onFocusChanged { focus ->
                val wasFocused = focused
                focused = focus.isFocused
                if (wasFocused && !focus.isFocused) commit()
            },
        placeholder = { Text(placeholder) },
    )
}

@Composable
private fun ModpackDescriptionPreview(value: String) {
    val textFieldState = rememberTextFieldState(value)
    LaunchedEffect(value) {
        if (textFieldState.text.toString() != value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }
    TextArea(
        textFieldState,
        readOnly = true,
        modifier = Modifier.fillMaxWidth().height(ModpackDescriptionInputHeight),
    )
}

@Composable
fun ModpackChoiceCombo(
    label: String,
    options: List<String>,
    selected: String,
    publish: (ModpackMsg) -> Unit,
    model: ModpackTabModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
    fieldKey: String? = null,
    validateChoice: (ConfigFile, LockFile, String) -> List<ActionError> = { _, _, _ -> emptyList() },
    fieldErrors: List<ActionError> = emptyList(),
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = FieldVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModpackFieldLabel(label)

        if (model.editingModpack) {
            val selectedIndex = options.indexOfFirst { it.equals(selected, ignoreCase = true) }
                .coerceAtLeast(0)
            ListComboBox(
                items = options,
                selectedIndex = selectedIndex,
                onSelectedItemChange = { index ->
                    val option = options.getOrNull(index) ?: return@ListComboBox
                    if (option.equals(selected, ignoreCase = true)) return@ListComboBox
                    publish(
                        ModpackMsg.MetaWriteRequested(
                            MetaWrite(
                                fieldKey = fieldKey,
                                validate = { config, lock -> validateChoice(config, lock, option) },
                                mutate = { config, lock -> mutate(config, lock, option) },
                            ),
                        ),
                    )
                },
                modifier = Modifier.width(240.dp).height(ModpackSingleLineInputHeight),
                listState = rememberListComboBoxState(options, selectedIndex),
            )
            ModpackFieldErrors(fieldErrors)
        } else if (selected.isBlank()) {
            MutedValue("—")
        } else {
            Text(selected, color = JewelTheme.contentColor, fontSize = 13.sp)
        }
        if (!model.editingModpack) ModpackFieldErrors(fieldErrors)
    }
}

@Composable
fun ModpackTargetChips(
    selected: String,
    publish: (ModpackMsg) -> Unit,
    model: ModpackTabModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
    fieldErrors: List<ActionError> = emptyList(),
) {
    ModpackChoiceCombo(
        label = "Target",
        options = ModpackFieldOptions.TARGETS,
        selected = selected,
        publish = publish,
        model = model,
        mutate = mutate,
        fieldKey = ModpackMetaField.TARGET,
        validateChoice = { _, lock, target -> lock.targetChangeErrors(target) },
        fieldErrors = fieldErrors,
    )
}

/**
 * Ordered string list shown as tags; editing adds an input row and per-tag remove.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackStringListField(
    label: String,
    entries: List<String>,
    editing: Boolean,
    emptyText: String,
    addPlaceholder: String,
    onChange: (List<String>) -> Unit,
    description: String? = null,
    monospace: Boolean = true,
    fieldErrors: List<ActionError> = emptyList(),
) {
    fun submit(next: List<String>) {
        onChange(next)
    }

    Column(
        Modifier.fillMaxWidth().padding(vertical = FieldVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModpackFieldLabel(label)
        description?.let { ModpackFieldHint(it) }

        if (entries.isEmpty() && !editing) {
            MutedValue(emptyText)
        } else if (entries.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                entries.forEach { entry ->
                    ValueTag(
                        text = entry,
                        monospace = monospace,
                        onRemove = if (editing) ({ submit(entries - entry) }) else null,
                    )
                }
            }
        }

        if (editing) {
            val input = rememberTextFieldState()
            fun add() {
                val text = input.text.toString().trim()
                if (text.isEmpty() || text in entries) return
                submit(entries + text)
                input.clearText()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    input,
                    Modifier
                        .width(280.dp)
                        .height(ModpackSingleLineInputHeight)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Enter, Key.NumPadEnter -> {
                                    add()
                                    true
                                }
                                else -> false
                            }
                        },
                    placeholder = { Text(addPlaceholder) },
                )
                OutlinedButton(onClick = ::add, enabled = input.text.isNotBlank()) {
                    Text("Add")
                }
            }
        }

        ModpackFieldErrors(fieldErrors)
    }
}

@Composable
private fun ValueTag(text: String, monospace: Boolean, onRemove: (() -> Unit)?) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .border(1.dp, JewelTheme.contentColor.copy(alpha = 0.22f), shape)
            .padding(start = 8.dp, end = if (onRemove != null) 4.dp else 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onRemove != null) {
            Icon(
                key = AllIconsKeys.Actions.Close,
                contentDescription = "Remove $text",
                tint = JewelTheme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove,
                    ),
            )
        }
    }
}

@Composable
fun ModpackFieldErrors(errors: List<ActionError>, modifier: Modifier = Modifier) {
    if (errors.isEmpty()) return
    Column(
        modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        errors.forEach { ActionErrorContent(it) }
    }
}

/** `loaders` of `pakku-lock.json`: loader name → version, in file order. */
@Composable
fun ModpackLoadersField(
    loaders: List<Pair<String, String>>,
    editing: Boolean,
    onChange: (List<Pair<String, String>>) -> Unit,
    fieldErrors: List<ActionError> = emptyList(),
) {
    fun submit(next: List<Pair<String, String>>) {
        onChange(next)
    }

    Column(
        Modifier.fillMaxWidth().padding(vertical = FieldVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModpackFieldLabel("Loaders")

        if (loaders.isEmpty() && !editing) MutedValue("No loaders")

        loaders.forEachIndexed { index, (name, version) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(LoaderNameWidth),
                )
                if (editing) {
                    CommitTextField(
                        value = version,
                        placeholder = "Version",
                        modifier = Modifier.width(184.dp),
                        onCommit = { text ->
                            submit(loaders.toMutableList().also { it[index] = name to text.trim() })
                        },
                    )
                    ToolbarActionButton(
                        key = AllIconsKeys.General.Delete,
                        label = "Remove $name",
                        onClick = { submit(loaders.filterIndexed { i, _ -> i != index }) },
                    )
                } else if (version.isBlank()) {
                    MutedValue("No version")
                } else {
                    Text(version, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        val available = ModpackFieldOptions.LOADERS.filter { option -> loaders.none { it.first == option } }
        if (editing && available.isNotEmpty()) {
            var addIndex by remember(available) { mutableIntStateOf(0) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListComboBox(
                    items = available,
                    selectedIndex = addIndex,
                    onSelectedItemChange = { addIndex = it },
                    modifier = Modifier
                        .width(LoaderNameWidth + 8.dp + 184.dp)
                        .height(ModpackSingleLineInputHeight),
                    listState = rememberListComboBoxState(available, addIndex),
                )
                OutlinedButton(onClick = { submit(loaders + (available[addIndex] to "")) }) {
                    Text("Add loader")
                }
            }
        }

        ModpackFieldErrors(fieldErrors)
    }
}

@Composable
fun ModpackCheckboxField(
    label: String,
    description: String,
    checked: Boolean,
    editing: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = FieldVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CheckboxRow(
            text = label,
            checked = checked,
            onCheckedChange = onChange,
            enabled = editing,
        )
        Row {
            // Lines the hint up with the checkbox label.
            Spacer(Modifier.width(24.dp))
            ModpackFieldHint(description)
        }
    }
}

@Composable
fun ModpackReadOnlyField(label: String, value: String?, monospace: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = JewelTheme.contentColor.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(LoaderNameWidth),
        )
        if (value.isNullOrBlank()) {
            MutedValue("—")
        } else {
            Text(
                value,
                fontSize = 13.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            )
        }
    }
}
