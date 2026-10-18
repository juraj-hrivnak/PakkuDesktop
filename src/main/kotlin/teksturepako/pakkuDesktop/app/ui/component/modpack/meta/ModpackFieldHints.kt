/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import teksturepako.pakku.api.projects.ProjectType

/** Cards of the Overview form, in display order. */
enum class ModpackIsland(val title: String, val description: String, val wide: Boolean = false)
{
    General("General", "Name, version, author, and description."),
    Minecraft("Minecraft", "Target platform, game versions, and loaders, stored in pakku-lock.json."),
    Export("Export", "What goes into exported modpacks."),
    Paths("Paths", "Where each project type is installed, relative to the modpack folder."),
    Projects("Project Overrides", "Properties overridden for individual projects.", wide = true),
    Parent("Parent modpack", "The modpack this one is forked from."),
    Other("Other", "Settings without a dedicated layout yet."),
    RawJson("Raw JSON", "Keys Pakku Desktop doesn't recognize, edited as JSON.", wide = true),
}

/** Editor a field is rendered with. */
enum class FieldWidget
{
    Text,
    Multiline,
    Number,
    Boolean,
    Choice,
    StringList,
    StringMap,
    Object,
    ObjectMap,
    RawJson,
}

/**
 * UI metadata for one field. Every property is optional: a field without a hint is labelled from
 * its serial name, lands in [ModpackIsland.Other], and gets the widget of its serial type.
 */
data class FieldHint(
    val label: String? = null,
    val description: String? = null,
    val island: ModpackIsland? = null,
    val order: Int = Int.MAX_VALUE,
    val widget: FieldWidget? = null,
    val placeholder: String = "",
    /** [FieldWidget.Choice] options, or the suggested keys of a [FieldWidget.StringMap]. */
    val options: List<String> = emptyList(),
    val optionLabels: Map<String, String> = emptyMap(),
    /** [FieldWidget.StringMap] value placeholder per key; falls back to [placeholder]. */
    val optionPlaceholders: Map<String, String> = emptyMap(),
    /** [FieldWidget.StringMap] shows a row for every option; a blank value removes the key. */
    val pinOptions: Boolean = false,
    /** Blank text is not committed. */
    val required: Boolean = false,
    /** Shown instead of a blank value. */
    val emptyText: String = "",
    /** Rendered as the hero title of the tab instead of inside an island. */
    val title: Boolean = false,
    /** Shown as badges under the title. */
    val summary: Boolean = false,
    /** Derived or managed by Pakku itself; not shown in the form. */
    val excluded: Boolean = false,
    /** Object-map entries are projects and use the Projects-tab card. */
    val projectCard: Boolean = false,
    /** A value can be cleared back to the default. */
    val resettable: Boolean = true,
    /** [FieldWidget.StringMap] entries are pills with an add chip, rather than labelled rows. */
    val chips: Boolean = false,
)

/**
 * Hints by file and field path. A path is the serial names from the top-level field down; map
 * entries are skipped, so `projects.side` covers the `side` of every project override.
 */
object ModpackFieldHints
{
    private val config: Map<String, FieldHint> = mapOf(
        "name" to FieldHint(
            label = "Name",
            title = true,
            required = true,
            emptyText = "Untitled modpack",
            resettable = false,
        ),
        "version" to FieldHint(
            label = "Version",
            island = ModpackIsland.General,
            order = 0,
            summary = true,
            placeholder = "e.g. 1.0.0",
            resettable = false,
        ),
        "author" to FieldHint(
            label = "Author",
            island = ModpackIsland.General,
            order = 1,
            summary = true,
            resettable = false,
        ),
        "description" to FieldHint(
            label = "Description",
            island = ModpackIsland.General,
            order = 2,
            widget = FieldWidget.Multiline,
            resettable = false,
        ),

        "export_server_side_projects_to_client" to FieldHint(
            label = "Export server-side projects to client",
            description = "Include projects with side SERVER in client exports.",
            island = ModpackIsland.Export,
            order = 0,
        ),
        "overrides" to FieldHint(
            label = "Overrides",
            description = "Copied into every export.",
            island = ModpackIsland.Export,
            order = 1,
            placeholder = "e.g. config/**",
        ),
        "server_overrides" to FieldHint(
            label = "Server overrides",
            description = "Copied into server exports only.",
            island = ModpackIsland.Export,
            order = 2,
            placeholder = "e.g. config/**",
        ),
        "client_overrides" to FieldHint(
            label = "Client overrides",
            description = "Copied into client exports only.",
            island = ModpackIsland.Export,
            order = 3,
            placeholder = "e.g. config/**",
        ),

        "paths" to FieldHint(
            label = "Project paths",
            description = "Leave blank to use the default.",
            island = ModpackIsland.Paths,
            options = ProjectType.entries.map { it.serialName },
            optionLabels = ProjectType.entries.associate { type ->
                type.serialName to type.prettyName.replaceFirstChar { it.titlecase() } + "s"
            },
            optionPlaceholders = ProjectType.entries.associate { it.serialName to it.getPathString(null) },
            pinOptions = true,
        ),

        "parent" to FieldHint(
            label = "",
            description = "Set a repository to link it.",
            island = ModpackIsland.Parent,
            order = 0,
        ),
        "parent.type" to FieldHint(label = "Type", placeholder = "git"),
        "parent.id" to FieldHint(label = "Repository", placeholder = "owner/name or git URL"),
        "parent.version" to FieldHint(label = "Version"),
        "parent.ref" to FieldHint(label = "Ref", placeholder = "main"),
        "parent.ref_type" to FieldHint(
            label = "Ref type",
            optionLabels = mapOf("BRANCH" to "Branch", "TAG" to "Tag", "COMMIT" to "Commit"),
        ),
        "parent.remote_name" to FieldHint(label = "Remote", placeholder = "origin"),
        "excludes" to FieldHint(
            label = "Excludes",
            description = "Parent projects left out of this modpack.",
            island = ModpackIsland.Parent,
            order = 1,
            placeholder = "Project slug, name, or Pakku ID",
        ),
        // Written by `pakku sync` to detect parent changes.
        "parent_lock_hash" to FieldHint(excluded = true),
        "parent_config_hash" to FieldHint(excluded = true),

        "projects" to FieldHint(
            label = "",
            island = ModpackIsland.Projects,
            order = 0,
            placeholder = "Project slug, name, or Pakku ID",
            emptyText = "None",
            projectCard = true,
        ),
        "projects.type" to FieldHint(label = "Type:"),
        "projects.side" to FieldHint(label = "Side:"),
        "projects.update_strategy" to FieldHint(label = "Update Strategy:"),
        "projects.redistributable" to FieldHint(label = "Redistributable:"),
        "projects.subpath" to FieldHint(label = "Subpath:"),
        "projects.aliases" to FieldHint(label = "Aliases:", placeholder = "Alias"),
        "projects.export" to FieldHint(label = "Export:"),
    )

    private val lock: Map<String, FieldHint> = mapOf(
        "target" to FieldHint(
            label = "Target",
            island = ModpackIsland.Minecraft,
            order = 0,
            widget = FieldWidget.Choice,
            options = ModpackFieldOptions.TARGETS,
            summary = true,
        ),
        "mc_versions" to FieldHint(
            label = "Minecraft versions",
            island = ModpackIsland.Minecraft,
            order = 1,
            placeholder = "e.g. 1.20.1",
            summary = true,
        ),
        "loaders" to FieldHint(
            label = "Loaders",
            island = ModpackIsland.Minecraft,
            order = 2,
            options = ModpackFieldOptions.LOADERS,
            placeholder = "Version",
            summary = true,
            chips = true,
        ),
        // Managed by add / remove / update and by Pakku migrations.
        "projects" to FieldHint(excluded = true),
        "lockfile_version" to FieldHint(excluded = true),
    )

    fun hint(file: ModpackFile, path: String): FieldHint? = when (file)
    {
        ModpackFile.Config -> config[path]
        ModpackFile.Lock -> lock[path]
    }

    /** Hinted paths, for checking that every hint still names a real field. */
    internal fun paths(file: ModpackFile): Set<String> = when (file)
    {
        ModpackFile.Config -> config.keys
        ModpackFile.Lock -> lock.keys
    }
}
