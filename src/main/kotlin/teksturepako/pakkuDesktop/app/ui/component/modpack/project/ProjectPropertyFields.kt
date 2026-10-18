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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectOverrideCommitErrors
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.subpathEditorText
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
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

private const val EmptyValue = "Not set"

/** Properties of one `pakku.json` project override. */
val modpackProjectConfigFields: List<ModpackField>
    get() = teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema.fields
        .first { it.serialName == "projects" }
        .fields

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
    // An explicit aliases array, including empty, replaces the lock-file aliases.
    if (field.serialName == "aliases" && override is JsonArray) return override
    val chosen = if (override.isOverride()) override else projectEffectiveConfigField(project, field)
    return if (field.serialName == "subpath" && chosen.text.trim() == ".") JsonNull else chosen
}

fun showProjectPropertyReadOnly(project: Project, field: ModpackField, override: JsonElement): Boolean {
    if (field.serialName == "subpath" && override.text.trim() == ".") return false
    if (field.serialName == "aliases" && override is JsonArray) return override.strings.isNotEmpty()
    if (override.isOverride()) return true
    return when (field.serialName) {
        "type", "side", "update_strategy", "redistributable" -> true
        "subpath" -> {
            val subpath = project.getSubpathOrNull()
            !subpath.isNullOrBlank() && subpath != "."
        }
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
    subpath = project.getSubpathOrNull()?.takeUnless { it.isBlank() || it == "." },
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
    write: (JsonElement?) -> Unit,
) {
    when (field.widget) {
        FieldWidget.Choice -> ProjectPropertyChoices(field, value, field.options, write) { field.textValue(it) }
        FieldWidget.Boolean -> ProjectPropertyChoices(field, value, listOf("true", "false"), write) {
            JsonPrimitive(it.toBoolean())
        }
        FieldWidget.Text ->
            if (field.serialName == "subpath") ProjectSubpathField(field, value, errors, write)
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
 * The characters in the field are only a draft. Nothing is written while it is focused,
 * so a legal prefix like `.` is not saved on the way to `..`.
 * On a real focus loss the current text is checked once: invalid input stays on screen and is not saved.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalCoroutinesApi::class)
@Composable
private fun ProjectSubpathField(
    field: ModpackField,
    value: JsonElement,
    errors: List<ActionError>,
    write: (JsonElement?) -> Unit,
) {
    RecomposeProbe("ProjectSubpathField")
    val state = rememberTextFieldState(subpathEditorText(value.text))
    val saved = rememberUpdatedState(subpathEditorText(value.text))
    var focused by remember { mutableStateOf(false) }
    var localErrors by remember { mutableStateOf<List<ActionError>>(emptyList()) }
    var skipCommit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { focused to state.text.toString() }
            .collectLatest { (isFocused, raw) ->
                val typed = raw.trim()
                val typedErrors = projectOverrideCommitErrors(field, typed)
                if (isFocused) {
                    localErrors = typedErrors
                    return@collectLatest
                }
                if (skipCommit) {
                    skipCommit = false
                    return@collectLatest
                }
                delay(150)
                if (focused || skipCommit) return@collectLatest
                val next = subpathEditorText(state.text.toString())
                val commitErrors = projectOverrideCommitErrors(field, state.text.toString())
                if (commitErrors.isNotEmpty()) {
                    localErrors = commitErrors
                    return@collectLatest
                }
                localErrors = emptyList()
                if (next == saved.value) return@collectLatest
                write(if (next.isEmpty()) null else field.textValue(next))
            }
    }
    val shownErrors = localErrors.ifEmpty { errors }
    ProjectPropertyTextRow(
        field = field,
        state = state,
        write = write,
        errors = shownErrors,
        textFieldModifier = Modifier.onFocusChanged { focused = it.isFocused },
        onReset = {
            skipCommit = true
            localErrors = emptyList()
        },
    )
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
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing)) {
                TextField(state, textFieldModifier, placeholder = { Text(field.hint.placeholder) })
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
