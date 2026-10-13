/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.metaFieldErrors

@Composable
fun ModpackMetaProperties(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    val config = model.configFile?.get() ?: return
    val lock = model.lockFile?.get() ?: return

    val name = config.getName()
    val version = config.getVersion()
    val author = config.getAuthor()
    val description = config.getDescription()
    val mcVersions = lock.getMcVersions().toList()
    val loaders = lock.getLoadersWithVersions()
    val target = lock.getProjectProvider().get()?.serialName.orEmpty()
    val editing = model.editingModpack

    fun write(request: MetaWrite) = publish(ModpackMsg.MetaWriteRequested(request))

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Modpack",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit: ", fontSize = 13.sp)
                Switch(
                    checked = model.editingModpack,
                    onCheckedChange = { publish(ModpackMsg.ModpackEditing(it)) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroNameField(
                value = name,
                editing = model.editingModpack,
                publish = publish,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    ModpackCommitTextField(
                        label = "Version",
                        value = version,
                        placeholder = "No version",
                        publish = publish,
                        model = model,
                        mutate = { cfg, _, text -> cfg.setVersion(text) },
                    )
                }
                Box(Modifier.weight(1f)) {
                    ModpackCommitTextField(
                        label = "Author",
                        value = author,
                        placeholder = "No author",
                        publish = publish,
                        model = model,
                        mutate = { cfg, _, text -> cfg.setAuthor(text) },
                    )
                }
            }
            if (!editing && (mcVersions.isNotEmpty() || loaders.isNotEmpty() || target.isNotBlank())) {
                Text(
                    listOfNotNull(
                        mcVersions.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "MC "),
                        loaders.takeIf { it.isNotEmpty() }?.joinToString(", ") { (loader, ver) ->
                            if (ver.isBlank()) loader else "$loader $ver"
                        },
                        target.ifBlank { null },
                    ).joinToString(" · "),
                    color = JewelTheme.contentColor.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            ModpackCommitTextField(
                label = "Description",
                value = description,
                placeholder = "No description",
                singleLine = false,
                publish = publish,
                model = model,
                mutate = { cfg, _, text -> cfg.setDescription(text) },
            )
        }

        ModpackSection("Minecraft", "pakku-lock.json — what projects are resolved against.") {
            ModpackTargetChips(
                selected = target,
                publish = publish,
                model = model,
                fieldErrors = model.metaFieldErrors(ModpackMetaField.TARGET),
                mutate = { _, lk, text ->
                    if (text.isNotBlank()) lk.setTarget(text.trim().lowercase())
                },
            )
            ModpackStringListField(
                label = "Minecraft versions",
                description = "The first version is the primary one; the rest are also accepted for project files.",
                entries = mcVersions,
                editing = editing,
                emptyText = "No Minecraft versions",
                addPlaceholder = "e.g. 1.20.1",
                fieldErrors = model.metaFieldErrors(ModpackMetaField.MC_VERSIONS),
                onChange = { next ->
                    write(
                        MetaWrite(
                            fieldKey = ModpackMetaField.MC_VERSIONS,
                            validate = { _, lk -> lk.mcVersionsChangeErrors(next) },
                            mutate = { _, lk -> lk.setMcVersions(next) },
                        ),
                    )
                },
            )
            ModpackLoadersField(
                loaders = loaders,
                editing = editing,
                fieldErrors = model.metaFieldErrors(ModpackMetaField.LOADERS),
                onChange = { next ->
                    write(
                        MetaWrite(
                            fieldKey = ModpackMetaField.LOADERS,
                            validate = { _, lk -> lk.loadersChangeErrors(next.map { it.first }) },
                            mutate = { _, lk -> lk.setLoaders(linkedMapOf(*next.toTypedArray())) },
                        ),
                    )
                },
            )
        }

        ModpackSection("Export", "pakku.json — what goes into exported modpacks.") {
            ModpackCheckboxField(
                label = "Export server-side projects to client",
                description = "Include server-only projects in client exports.",
                checked = config.getExportServerSideProjectsToClient(),
                editing = editing,
                onChange = { checked ->
                    write(MetaWrite { cfg, _ -> cfg.setExportServerSideProjectsToClient(checked) })
                },
            )
            OverrideList.entries.forEach { list ->
                ModpackStringListField(
                    label = list.title,
                    description = "${list.description} Paths and globs, relative to the modpack.",
                    entries = config.overrideEntries(list),
                    editing = editing,
                    emptyText = "None",
                    addPlaceholder = "e.g. config/**",
                    onChange = { next -> write(MetaWrite(replaceConfig = { it.withOverrideEntries(list, next) })) },
                )
            }
        }

        ModpackSection("Project paths", "pakku.json — where each project type is placed. Leave blank for the default.") {
            ProjectType.entries.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { type ->
                        Box(Modifier.weight(1f)) {
                            ModpackCommitTextField(
                                label = type.prettyName.replaceFirstChar { it.titlecase() } + "s",
                                value = config.paths[type.serialName].orEmpty(),
                                placeholder = type.getPathString(null),
                                publish = publish,
                                model = model,
                                mutate = { cfg, _, text ->
                                    val path = text.trim()
                                    if (path.isEmpty()) cfg.paths.remove(type.serialName) else cfg.paths[type.serialName] = path
                                },
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        val parent = config.parent
        if (parent != null || config.excludes.isNotEmpty()) {
            ModpackSection("Parent modpack", "pakku.json — the modpack this one is synced from.") {
                if (parent != null) {
                    ModpackReadOnlyField("Source", parent.type)
                    ModpackReadOnlyField("Repository", parent.id, monospace = true)
                    ModpackReadOnlyField("Ref", "${parent.ref} (${parent.refType.name.lowercase()})", monospace = true)
                    ModpackReadOnlyField("Remote", parent.remoteName, monospace = true)
                    ModpackReadOnlyField("Version", parent.version, monospace = true)
                }
                ModpackStringListField(
                    label = "Excludes",
                    description = "Parent projects left out of this modpack.",
                    entries = config.excludes.toList(),
                    editing = editing,
                    emptyText = "None",
                    addPlaceholder = "Project slug, name, or Pakku ID",
                    onChange = { next ->
                        write(
                            MetaWrite { cfg, _ ->
                                cfg.excludes.clear()
                                cfg.excludes.addAll(next)
                            },
                        )
                    },
                )
            }
        }
    }
}

internal val ModpackContentMaxWidth = 760.dp

@Composable
private fun ModpackSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GroupHeader(title)
        ModpackFieldHint(description)
        content()
    }
}

@Composable
private fun HeroNameField(
    value: String,
    editing: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    if (editing) {
        val textFieldState = rememberTextFieldState(value)
        var focused by remember { mutableStateOf(false) }

        LaunchedEffect(value) {
            if (!focused && textFieldState.text.toString() != value) {
                textFieldState.setTextAndPlaceCursorAtEnd(value)
            }
        }

        fun commit() {
            val text = textFieldState.text.toString()
            if (text == value) return
            publish(
                ModpackMsg.MetaWriteRequested(
                    MetaWrite { config, _ -> config.setName(text) },
                ),
            )
        }

        LaunchModpackAutoSave(value, textFieldState, onCommit = { commit() })

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Name",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            TextField(
                textFieldState,
                Modifier
                    .fillMaxWidth()
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
            )
        }
    } else {
        Text(
            value.ifBlank { "Untitled modpack" },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (value.isBlank()) {
                JewelTheme.contentColor.copy(alpha = 0.4f)
            } else {
                JewelTheme.contentColor
            },
        )
    }
}
