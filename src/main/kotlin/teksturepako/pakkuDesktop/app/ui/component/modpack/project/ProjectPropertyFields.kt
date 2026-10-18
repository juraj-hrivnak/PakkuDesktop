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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.isOverride
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectOverrideCommitErrors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.icon.IconKey
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
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

private const val EmptyValue = "Not set"

@OptIn(ExperimentalSerializationApi::class)
private val projectConfigJson = Json(json) {
    encodeDefaults = true
    explicitNulls = true
}

/** Lock-file value for one `projects.*` field, encoded like `pakku.json`. */
fun projectEffectiveConfigField(project: Project, field: ModpackField): JsonElement {
    val encoded = projectConfigJson.encodeToJsonElement(
        ConfigFile.ProjectConfig.serializer(),
        projectToProjectConfig(project),
    ).jsonObject
    return encoded[field.serialName] ?: JsonNull
}

/** Stored override when present; otherwise the effective lock-file default (for editors and read-only). */
fun projectPropertyUiValue(project: Project, field: ModpackField, override: JsonElement): JsonElement {
    // Same source as file paths: [Project.getSubpath], after config inheritance.
    if (field.serialName == "subpath") {
        val subpath = project.getSubpathOrNull()?.takeUnless { it.isBlank() }
        return if (subpath == null) JsonNull else JsonPrimitive(subpath)
    }
    // An explicit aliases array, including empty, replaces the lock-file aliases.
    if (field.serialName == "aliases" && override is JsonArray) return override
    val chosen = if (override.isOverride()) override else projectEffectiveConfigField(project, field)
    return chosen
}

fun showProjectPropertyReadOnly(project: Project, field: ModpackField, override: JsonElement): Boolean {
    if (field.serialName == "aliases" && override is JsonArray) return override.strings.isNotEmpty()
    if (override.isOverride()) return true
    return when (field.serialName) {
        "type", "side", "update_strategy", "redistributable" -> true
        "subpath" -> !project.getSubpathOrNull().isNullOrBlank()
        "aliases" -> !project.aliases.isNullOrEmpty()
        "export" -> true
        else -> false
    }
}

/** Pakku export rules only skip a project when `export == false`; unset means included. */
private fun projectExportEffective(project: Project): Boolean = project.export ?: true

private fun projectToProjectConfig(project: Project) = ConfigFile.ProjectConfig(
    type = project.type,
    side = project.side,
    updateStrategy = project.updateStrategy,
    redistributable = project.redistributable,
    subpath = project.getSubpathOrNull()?.takeUnless { it.isBlank() },
    aliases = project.aliases?.takeIf { it.isNotEmpty() },
    export = projectExportEffective(project),
)

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
    errors: List<ActionError> = emptyList(),
    onSubpathFocus: (Boolean) -> Unit = {},
    write: (JsonElement?) -> Unit,
) {
    when (field.widget) {
        FieldWidget.Choice -> ProjectPropertyChoices(field, value, field.options, write) { field.textValue(it) }
        FieldWidget.Boolean -> ProjectPropertyChoices(field, value, listOf("true", "false"), write) {
            JsonPrimitive(it.toBoolean())
        }
        FieldWidget.Text ->
            if (field.serialName == "subpath") ProjectSubpathField(field, value, errors, onSubpathFocus, write)
            else ProjectPropertyText(field, value, write)
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
    ProjectPropertyTextRow(field, state, write)
}

/**
 * The characters in the field are a draft. Check or Enter writes them.
 * X or Esc restores the last saved subpath. An invalid path is not written.
 */
@Composable
private fun ProjectSubpathField(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    onFocus: (Boolean) -> Unit,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ProjectSubpathField")
    val external = value.text.trim()
    val savedExternal = rememberUpdatedState(external)
    val state = rememberTextFieldState(external)
    var focused by remember { mutableStateOf(false) }
    var localErrors by remember { mutableStateOf<List<ActionError>>(emptyList()) }
    val onFocusChanged = rememberUpdatedState(onFocus)

    fun revertDraft() {
        localErrors = emptyList()
        if (state.text.toString().trim() != savedExternal.value) {
            state.setTextAndPlaceCursorAtEnd(savedExternal.value)
        }
    }

    fun commitDraft() {
        val next = state.text.toString().trim()
        val commitErrors = projectOverrideCommitErrors(field, next)
        if (commitErrors.isNotEmpty()) {
            localErrors = commitErrors
            return
        }
        localErrors = emptyList()
        if (next == savedExternal.value) return
        write(if (next.isEmpty()) null else field.textValue(next))
    }

    DisposableEffect(Unit) {
        onDispose { onFocusChanged.value(false) }
    }
    LaunchedEffect(external) {
        if (!focused && localErrors.isEmpty() && state.text.toString().trim() != external) {
            state.setTextAndPlaceCursorAtEnd(external)
        }
    }
    LaunchedEffect(focused, state.text.toString()) {
        if (focused) {
            localErrors = projectOverrideCommitErrors(field, state.text.toString().trim())
        }
    }
    val shownErrors = localErrors.ifEmpty { errors }
    ProjectPropertyTextRow(
        field = field,
        state = state,
        write = write,
        errors = shownErrors,
        textFieldModifier = Modifier
            .onFocusChanged { change ->
                focused = change.isFocused
                onFocus(change.isFocused)
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        commitDraft()
                        true
                    }
                    Key.Escape -> {
                        revertDraft()
                        true
                    }
                    else -> false
                }
            },
        onReset = { localErrors = emptyList() },
        trailing = { height ->
            SubpathActionButton(height, AllIconsKeys.Actions.Checked, "Apply", ::commitDraft)
            SubpathActionButton(height, AllIconsKeys.General.Close, "Cancel", ::revertDraft)
        },
    )
}

@Composable
private fun RowScope.SubpathActionButton(
    size: Dp,
    icon: IconKey,
    description: String,
    onClick: () -> Unit,
) {
    PkUiTooltip({ Text(description) }) {
        IconButton(onClick = onClick, focusable = false, modifier = Modifier.size(size)) {
            Icon(icon, description, tint = JewelTheme.contentColor)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectPropertyTextRow(
    field: ModpackField,
    state: androidx.compose.foundation.text.input.TextFieldState,
    write: (JsonElement?) -> Unit,
    errors: List<ActionError> = emptyList(),
    textFieldModifier: Modifier = Modifier,
    onReset: () -> Unit = {},
    trailing: (@Composable RowScope.(Dp) -> Unit)? = null,
) {
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
                                    onReset()
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
            Column(
                Modifier
                    .then(if (trailing != null) Modifier.weight(1f) else Modifier)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            ) {
                if (trailing == null) {
                    TextField(state, textFieldModifier, placeholder = { Text(field.hint.placeholder) })
                } else {
                    val density = LocalDensity.current
                    var inputHeight by remember { mutableStateOf(0.dp) }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
                    ) {
                        TextField(
                            state,
                            Modifier
                                .weight(1f)
                                .onSizeChanged { inputHeight = with(density) { it.height.toDp() } }
                                .then(textFieldModifier),
                            placeholder = { Text(field.hint.placeholder) },
                        )
                        if (inputHeight > 0.dp) trailing(inputHeight)
                    }
                }
                if (errors.isNotEmpty()) {
                    errors.forEach { ActionErrorContent(it) }
                }
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
        // Empty aliases must stay in pakku.json. Removing the key brings the lock-file aliases back.
        val stored = when {
            next.isEmpty() && field.serialName == "aliases" -> stringListValue(emptyList())
            next.isEmpty() -> null
            else -> stringListValue(next)
        }
        write(stored)
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
                                    local = null
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
