/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
import teksturepako.pakkuDesktop.app.ui.model.metaFieldErrors

@Composable
fun ModpackMetaProperties(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    val config = model.configFile?.get() ?: return
    val lock = model.lockFile?.get() ?: return
    val editing = model.editingModpack

    fun write(request: MetaWrite) = publish(ModpackMsg.MetaWriteRequested(request))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // GroupHeader’s endComponent slot is height-capped at 16.dp — keep Edit outside it.
            GroupHeader("Properties", modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit: ")
                Switch(
                    checked = editing,
                    onCheckedChange = {
                        if (!editing) {
                            publish(ModpackMsg.ModpackEditing(true))
                        } else {
                            publish(ModpackMsg.ModpackEditing(false))
                        }
                    },
                )
            }
        }

        ModpackStringSelection(
            label = "Name:",
            value = config.getName(),
            editing = editing,
            alwaysShow = true,
            emptyText = "Untitled modpack",
            onChange = { text -> write(MetaWrite { cfg, _ -> cfg.setName(text) }) },
        )
        ModpackStringSelection(
            label = "Version:",
            value = config.getVersion(),
            editing = editing,
            onChange = { text -> write(MetaWrite { cfg, _ -> cfg.setVersion(text) }) },
        )
        ModpackStringSelection(
            label = "Author:",
            value = config.getAuthor(),
            editing = editing,
            onChange = { text -> write(MetaWrite { cfg, _ -> cfg.setAuthor(text) }) },
        )
        ModpackStringSelection(
            label = "Description:",
            value = config.getDescription(),
            editing = editing,
            multiline = true,
            onChange = { text -> write(MetaWrite { cfg, _ -> cfg.setDescription(text) }) },
        )

        SectionHeader("Minecraft")
        ModpackChoiceSelection(
            label = "Target:",
            options = ModpackFieldOptions.TARGETS,
            selected = lock.getProjectProvider().get()?.serialName.orEmpty(),
            editing = editing,
            fieldErrors = model.metaFieldErrors(ModpackMetaField.TARGET),
            onSelect = { option ->
                write(
                    MetaWrite(
                        fieldKey = ModpackMetaField.TARGET,
                        validate = { _, lk -> lk.targetChangeErrors(option) },
                        mutate = { _, lk -> lk.setTarget(option.trim().lowercase()) },
                    ),
                )
            },
        )
        ModpackStringListSelection(
            label = "Minecraft versions:",
            entries = lock.getMcVersions().toList(),
            editing = editing,
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
        ModpackLoadersSelection(
            loaders = lock.getLoadersWithVersions(),
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

        SectionHeader("Export")
        ModpackBooleanSelection(
            label = "Export server-side projects to client:",
            checked = config.getExportServerSideProjectsToClient(),
            editing = editing,
            onChange = { checked ->
                write(MetaWrite { cfg, _ -> cfg.setExportServerSideProjectsToClient(checked) })
            },
        )
        OverrideList.entries.forEach { list ->
            ModpackStringListSelection(
                label = "${list.title}:",
                entries = config.overrideEntries(list),
                editing = editing,
                addPlaceholder = "e.g. config/**",
                onChange = { next -> write(MetaWrite(replaceConfig = { it.withOverrideEntries(list, next) })) },
            )
        }

        SectionHeader("Project paths")
        ProjectType.entries.forEach { type ->
            val current = config.paths[type.serialName].orEmpty()
            ModpackStringSelection(
                label = type.prettyName.replaceFirstChar { it.titlecase() } + "s:",
                value = current,
                editing = editing,
                placeholder = type.getPathString(null),
                onChange = { text ->
                    val path = text.trim()
                    if (path == current) return@ModpackStringSelection
                    write(
                        MetaWrite { cfg, _ ->
                            if (path.isEmpty()) cfg.paths.remove(type.serialName) else cfg.paths[type.serialName] = path
                        },
                    )
                },
            )
        }

        val parent = config.parent
        if (parent != null || editing || config.excludes.isNotEmpty()) {
            SectionHeader("Parent modpack")
            if (parent != null) {
                ModpackReadOnlySelection("Source:", parent.type)
                ModpackReadOnlySelection("Repository:", parent.id)
                ModpackReadOnlySelection("Ref:", "${parent.ref} (${parent.refType.name.lowercase()})")
                ModpackReadOnlySelection("Remote:", parent.remoteName)
                ModpackReadOnlySelection("Version:", parent.version)
            }
            ModpackStringListSelection(
                label = "Excludes:",
                entries = config.excludes.toList(),
                editing = editing,
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

@Composable
private fun SectionHeader(title: String) {
    GroupHeader(title, modifier = Modifier.padding(top = 12.dp))
}
