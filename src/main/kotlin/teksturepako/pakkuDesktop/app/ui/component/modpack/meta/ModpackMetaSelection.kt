/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

private val DescriptionInputHeight = 120.dp

/**
 * Pack metadata string. Same edit chrome as project properties: label chip, reset, and a text
 * field that writes as you type. Blank values stay hidden until Edit is on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackStringSelection(
    label: String,
    value: String,
    editing: Boolean,
    onChange: (String) -> Unit,
    placeholder: String = "",
    multiline: Boolean = false,
    resettable: Boolean = true,
    alwaysShow: Boolean = false,
    emptyText: String = "",
    belowLabel: (@Composable () -> Unit)? = null,
) {
    if (editing) {
        val textFieldState = rememberTextFieldState(value)
        // Skip the initial composition value — opening Edit must not rewrite the pack.
        var seeded by remember { mutableStateOf(false) }

        LaunchedEffect(textFieldState.text) {
            val text = textFieldState.text.toString()
            if (!seeded) {
                seeded = true
                return@LaunchedEffect
            }
            if (text != value) onChange(text)
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            if (multiline) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    ModpackEditLabel(label, resettable, textFieldState, belowLabel)
                    TextArea(
                        textFieldState,
                        Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(DescriptionInputHeight),
                        placeholder = { Text(placeholder) },
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    ModpackEditLabel(label, resettable, textFieldState, belowLabel)
                    FlowRow(Modifier.padding(4.dp)) {
                        TextField(
                            textFieldState,
                            placeholder = { Text(placeholder) },
                        )
                    }
                }
            }
        }
    } else if (value.isNotBlank()) {
        ModpackValueRow(label, listOf(value), wrap = multiline)
    } else if (alwaysShow) {
        ModpackValueRow(label, listOf(value.ifBlank { emptyText }), muted = value.isBlank(), wrap = multiline)
    }
}

/** Discrete choice, edited with the same segmented buttons as a project enum. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackChoiceSelection(
    label: String,
    options: List<String>,
    selected: String,
    editing: Boolean,
    onSelect: (String) -> Unit,
    fieldErrors: List<ActionError> = emptyList(),
) {
    if (editing) {
        var buttonState by remember { mutableStateOf(selected) }
        LaunchedEffect(selected) { buttonState = selected }
        // A rejected write leaves the pack on the previous value — snap the buttons back with it.
        LaunchedEffect(fieldErrors) {
            if (fieldErrors.isNotEmpty()) buttonState = selected
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
                FlowRow(Modifier.padding(2.dp)) {
                    options.forEach { option ->
                        SegmentedControl(
                            listOf(
                                SegmentedControlButtonData(
                                    selected = buttonState.equals(option, ignoreCase = true),
                                    content = { _ -> Text(option) },
                                    onSelect = {
                                        buttonState = option
                                        if (!option.equals(selected, ignoreCase = true)) onSelect(option)
                                    },
                                ),
                            ),
                            Modifier.padding(2.dp),
                        )
                    }
                }
            }
            ModpackFieldErrors(fieldErrors)
        }
    } else if (selected.isNotBlank()) {
        ModpackValueRow(label, listOf(selected))
        ModpackFieldErrors(fieldErrors)
    } else {
        ModpackFieldErrors(fieldErrors)
    }
}

/** Boolean pack flag, edited with the same true/false segments as a project boolean. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackBooleanSelection(
    label: String,
    checked: Boolean,
    editing: Boolean,
    onChange: (Boolean) -> Unit,
) {
    if (editing) {
        var buttonState by remember { mutableStateOf(checked) }

        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
                FlowRow(Modifier.padding(2.dp)) {
                    listOf(true, false).forEach { value ->
                        SegmentedControl(
                            listOf(
                                SegmentedControlButtonData(
                                    selected = buttonState == value,
                                    content = { _ -> Text(value.toString()) },
                                    onSelect = {
                                        buttonState = value
                                        onChange(value)
                                    },
                                ),
                            ),
                            Modifier.padding(2.dp),
                        )
                    }
                }
            }
        }
    } else {
        ModpackValueRow(label, listOf(checked.toString()))
    }
}

/**
 * Ordered string list. View mode is label + value chips; Edit adds a field and a remove button
 * on each chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackStringListSelection(
    label: String,
    entries: List<String>,
    editing: Boolean,
    addPlaceholder: String,
    onChange: (List<String>) -> Unit,
    fieldErrors: List<ActionError> = emptyList(),
) {
    if (!editing) {
        if (entries.isNotEmpty()) ModpackValueRow(label, entries)
        ModpackFieldErrors(fieldErrors)
        return
    }

    val input = rememberTextFieldState()

    fun add() {
        val text = input.text.toString().trim()
        if (text.isEmpty() || text in entries) return
        onChange(entries + text)
        input.clearText()
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
            FlowRow(Modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
                entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ContentBox(Modifier.padding(2.dp)) { Text(entry) }
                        IconButton(
                            onClick = { onChange(entries - entry) },
                            modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                        ) {
                            Icon(
                                AllIconsKeys.Actions.Close,
                                "Remove $entry",
                                tint = JewelTheme.contentColor,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                TextField(
                    input,
                    Modifier.onPreviewKeyEvent { event ->
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
                PkUiTooltip({ Text("Add") }) {
                    IconButton(
                        onClick = ::add,
                        enabled = input.text.isNotBlank(),
                        modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                    ) {
                        Icon(
                            AllIconsKeys.General.Add,
                            "Add",
                            tint = JewelTheme.contentColor,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
        ModpackFieldErrors(fieldErrors)
    }
}

/** `loaders` of `pakku-lock.json`: loader name → version, in file order. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackLoadersSelection(
    loaders: List<Pair<String, String>>,
    editing: Boolean,
    onChange: (List<Pair<String, String>>) -> Unit,
    fieldErrors: List<ActionError> = emptyList(),
) {
    if (!editing) {
        val labels = loaders.map { (name, version) ->
            if (version.isBlank()) name else "$name $version"
        }
        if (labels.isNotEmpty()) ModpackValueRow("Loaders:", labels)
        ModpackFieldErrors(fieldErrors)
        return
    }

    Column(Modifier.fillMaxWidth()) {
        loaders.forEach { (name, version) ->
            key(name) {
                ModpackStringSelection(
                    label = "$name:",
                    value = version,
                    editing = true,
                    resettable = false,
                    placeholder = "Version",
                    onChange = { text ->
                        val trimmed = text.trim()
                        if (trimmed == version) return@ModpackStringSelection
                        onChange(loaders.map { if (it.first == name) name to trimmed else it })
                    },
                    belowLabel = {
                        PkUiTooltip({ Text("Remove $name") }) {
                            IconButton(
                                onClick = { onChange(loaders.filter { it.first != name }) },
                                modifier = Modifier.padding(horizontal = 4.dp).size(25.dp),
                            ) {
                                Icon(
                                    AllIconsKeys.General.Delete,
                                    "Remove $name",
                                    tint = JewelTheme.contentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                    },
                )
            }
        }

        val available = ModpackFieldOptions.LOADERS.filter { option -> loaders.none { it.first == option } }
        if (available.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Row { ContentBox(Modifier.padding(2.dp)) { Text(if (loaders.isEmpty()) "Loaders:" else "Add:") } }
                    FlowRow(Modifier.padding(2.dp)) {
                        available.forEach { name ->
                            SegmentedControl(
                                listOf(
                                    SegmentedControlButtonData(
                                        selected = false,
                                        content = { _ -> Text(name) },
                                        onSelect = { onChange(loaders + (name to "")) },
                                    ),
                                ),
                                Modifier.padding(2.dp),
                            )
                        }
                    }
                }
            }
        }
        ModpackFieldErrors(fieldErrors)
    }
}

@Composable
fun ModpackReadOnlySelection(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    ModpackValueRow(label, listOf(value))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModpackValueRow(
    label: String,
    values: List<String>,
    muted: Boolean = false,
    wrap: Boolean = false,
) {
    FlowRow(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
        ContentBox(Modifier.padding(2.dp)) { Text(label) }
        values.forEach { value ->
            ContentBox(
                Modifier
                    .padding(2.dp)
                    .then(if (wrap) Modifier.widthIn(max = 560.dp) else Modifier),
            ) {
                Text(
                    value,
                    color = if (muted) JewelTheme.contentColor.copy(alpha = 0.4f) else JewelTheme.contentColor,
                )
            }
        }
    }
}

@Composable
private fun ModpackEditLabel(
    label: String,
    resettable: Boolean,
    textFieldState: TextFieldState,
    belowLabel: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row { ContentBox(Modifier.padding(2.dp)) { Text(label) } }
        belowLabel?.invoke()
        if (resettable) {
            Row {
                PkUiTooltip({ Text("Reset to default") }) {
                    IconButton(
                        onClick = { textFieldState.edit { delete(0, length) } },
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
