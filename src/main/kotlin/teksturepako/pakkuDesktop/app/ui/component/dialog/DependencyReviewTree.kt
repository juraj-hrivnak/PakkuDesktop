/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.errors.NoFiles
import teksturepako.pakku.api.actions.errors.NoFilesOn
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.actions.errors.ProjRequiredBy
import teksturepako.pakku.api.actions.errors.VersionsDoNotMatch
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.DepNode
import teksturepako.pakkuDesktop.app.actions.RemovalEntry
import teksturepako.pakkuDesktop.app.actions.fingerprint
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef
import teksturepako.pakkuDesktop.pkui.component.PkUiChip
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSectionLabel

/** Width of the checkbox column — deps align to the content column beside it. */
private val CheckboxCol = 28.dp

/** Indent per nested dependency level. */
private val DepIndent = 16.dp

/**
 * Deep and cyclic trees used to keep indenting until the name and chip were crushed
 * against the right edge. Past this depth, rows stay put and a cycle is a leaf.
 */
private const val MaxVisualDepth = 8

private val DepIconSize = 14.dp

@Composable
fun ReviewRootRow(
    project: Project,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    replacing: Project? = null,
    warnings: List<ActionError> = emptyList(),
    shownFingerprints: MutableSet<String>,
    deps: List<DepNode> = emptyList(),
    depSectionLabel: String = "Also adds",
    depCue: String = "new",
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RootCheckRow(
            checked = checked,
            onCheckedChange = onCheckedChange,
        ) {
            if (replacing != null) {
                ProjectRef(
                    replacing,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                    ellipsis = true,
                )
                Icon(
                    AllIconsKeys.Vcs.Arrow_right,
                    contentDescription = "replaced by",
                    tint = JewelTheme.contentColor.copy(alpha = 0.55f),
                    modifier = Modifier.size(DepIconSize),
                )
                ProjectRef(
                    project,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                    ellipsis = true,
                )
            } else {
                ProjectRef(
                    project,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                    ellipsis = true,
                )
            }
        }

        ContentUnderRoot {
            warnings.forEach { warning ->
                if (replacing != null && warning is AlreadyAdded) return@forEach
                ContextualWarning(warning, shownFingerprints)
            }
            if (checked && deps.isNotEmpty()) {
                DepSection(
                    label = depSectionLabel,
                    nodes = deps,
                    cue = depCue,
                    shownFingerprints = shownFingerprints,
                )
            }
        }
    }
}

/**
 * Remove review row: each linked dep is individually checkable (CLI ynPrompt per dep).
 * Recommended (unused) deps default on; still-required deps default off with a warning.
 */
@Composable
fun ReviewRootRowSimple(
    project: Project,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    warning: ActionError? = null,
    shownFingerprints: MutableSet<String>,
    orphanedChildren: List<RemovalEntry> = emptyList(),
    acceptedDepIds: Set<String> = emptySet(),
    onDepCheckedChange: (key: String, checked: Boolean) -> Unit = { _, _ -> },
    depSectionLabel: String = "Dependencies",
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RootCheckRow(
            checked = checked,
            onCheckedChange = onCheckedChange,
        ) {
            ProjectRef(
                project,
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
                ellipsis = true,
            )
        }

        ContentUnderRoot {
            warning?.let { ContextualWarning(it, shownFingerprints) }
            if (checked && orphanedChildren.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PkUiDialogSectionLabel(
                        depSectionLabel,
                        icon = AllIconsKeys.Toolwindows.Dependencies,
                        trailing = orphanedChildren.size.toString(),
                    )
                    orphanedChildren.forEach { child ->
                        RemovableDepRow(
                            entry = child,
                            checked = child.key in acceptedDepIds,
                            onCheckedChange = { on -> onDepCheckedChange(child.key, on) },
                            shownFingerprints = shownFingerprints,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovableDepRow(
    entry: RemovalEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shownFingerprints: MutableSet<String>,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.width(CheckboxCol),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
            DependencyIcon(dimmed = !checked)
            ProjectRef(
                entry.project,
                fontSize = 12.sp,
                iconSize = 14.dp,
                ellipsis = true,
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
            )
            PkUiChip(
                text = if (entry.isRecommended) "unused" else "still required",
                muted = !entry.isRecommended,
                accent = if (entry.isRecommended) {
                    PakkuDesktopConstants.highlightColor
                } else {
                    PakkuDesktopConstants.amber
                },
            )
        }
        entry.warning?.let {
            ContextualWarning(
                it,
                shownFingerprints,
                Modifier.padding(start = CheckboxCol + DepIconSize + 12.dp),
            )
        }
    }
}

@Composable
private fun RootCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.width(CheckboxCol),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
            )
        }
        Row(
            Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

/** Content aligned under the project label (past the checkbox column). */
@Composable
private fun ContentUnderRoot(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = CheckboxCol + 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

@Composable
private fun DepSection(
    label: String,
    nodes: List<DepNode>,
    cue: String,
    shownFingerprints: MutableSet<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PkUiDialogSectionLabel(
            label,
            icon = AllIconsKeys.Toolwindows.Dependencies,
            trailing = nodes.countDeps().toString(),
        )
        DepTree(
            nodes = nodes,
            depth = 0,
            cue = cue,
            shownFingerprints = shownFingerprints,
        )
    }
}

private fun List<DepNode>.countDeps(): Int = sumOf { 1 + it.children.countDeps() }

/** Indent grows with depth, then stops so the name still has a column to live in. */
private fun depPad(depth: Int): Dp = DepIndent * depth.coerceIn(0, MaxVisualDepth)

@Composable
private fun DepTree(
    nodes: List<DepNode>,
    depth: Int,
    cue: String,
    shownFingerprints: MutableSet<String>,
    ancestors: Set<String> = emptySet(),
) {
    nodes.forEach { node ->
        val key = node.project.uiKey()
        // Data marks a back-edge; the set catches a loop the preview did not.
        val cycle = node.cycle || key in ancestors
        val indent = depPad(depth)
        DepRow(
            project = node.project,
            indent = indent,
            alreadyPresent = node.alreadyPresent,
            cycle = cycle,
            cue = cue,
        )
        if (!cycle) {
            node.warnings.forEach {
                ContextualWarning(
                    it,
                    shownFingerprints,
                    Modifier.padding(start = indent + DepIconSize + 8.dp),
                )
            }
            if (node.children.isNotEmpty()) {
                DepTree(
                    nodes = node.children,
                    depth = depth + 1,
                    cue = cue,
                    shownFingerprints = shownFingerprints,
                    ancestors = ancestors + key,
                )
            }
        }
    }
}

@Composable
private fun DepRow(
    project: Project,
    indent: Dp,
    alreadyPresent: Boolean,
    cycle: Boolean,
    cue: String,
) {
    val dimmed = alreadyPresent || cycle
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DependencyIcon(dimmed = dimmed)
        ProjectRef(
            project,
            fontSize = 12.sp,
            iconSize = 14.dp,
            ellipsis = true,
            modifier = Modifier.weight(1f, fill = false).widthIn(min = 0.dp),
        )
        PkUiChip(
            text = when {
                cycle -> "cycle"
                alreadyPresent -> "in pack"
                else -> cue
            },
            muted = dimmed,
        )
    }
}

/** New UI "related" link glyph marking a row as a dependency of the row above. */
@Composable
private fun DependencyIcon(dimmed: Boolean) {
    Icon(
        AllIconsKeys.Nodes.Related,
        contentDescription = "Dependency",
        tint = if (dimmed) {
            JewelTheme.contentColor.copy(alpha = 0.4f)
        } else {
            PakkuDesktopConstants.highlightColor
        },
        modifier = Modifier.size(DepIconSize),
    )
}

/**
 * Warning text without re-stating the project already shown on the parent row/leaf.
 * Skips fingerprints already rendered elsewhere in this dialog pass.
 */
@Composable
fun ContextualWarning(
    error: ActionError,
    shownFingerprints: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    val fp = error.fingerprint()
    if (!shownFingerprints.add(fp)) return

    val color = PakkuDesktopConstants.amber
    val muted = JewelTheme.contentColor.copy(alpha = 0.7f)
    when (error) {
        is ProjRequiredBy -> {
            FlowRow(
                modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AllIconsKeys.Nodes.Related,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(12.dp),
                )
                Text("Required by", color = muted, fontSize = 11.sp)
                val dependants = error.dependants
                dependants.forEachIndexed { i, dep ->
                    // Comma stays on the previous mod. Flow spacing must not open a gap before it.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProjectRef(dep, fontSize = 11.sp, iconSize = 12.dp, ellipsis = true)
                        if (i < dependants.lastIndex - 1) {
                            Text(",", color = muted, fontSize = 11.sp)
                        }
                    }
                    if (i == dependants.lastIndex - 1) {
                        Text("&", color = muted, fontSize = 11.sp)
                    }
                }
            }
        }
        is NotFoundOn -> WarningLine("Not found on ${error.provider.name}", color, modifier)
        is NoFilesOn -> WarningLine("No files on ${error.provider.name}", color, modifier)
        is NoFiles -> WarningLine("No matching files for this pack", color, modifier)
        is VersionsDoNotMatch -> WarningLine("Versions do not match across platforms", color, modifier)
        is AlreadyAdded -> Text(
            "Already in pack",
            color = muted,
            fontSize = 11.sp,
            modifier = modifier,
        )
        else -> ActionErrorContent(error, compact = true, modifier = modifier)
    }
}

@Composable
private fun WarningLine(text: String, color: Color, modifier: Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PakkuDesktopIcons.exclamationTriangle, contentDescription = null, modifier = Modifier.size(12.dp))
        Text(text, color = color, fontSize = 11.sp)
    }
}

fun Project.shortDisplayName(): String =
    name.values.firstOrNull()
        ?: slug.values.firstOrNull()
        ?: id.values.firstOrNull()
        ?: "project"

fun primaryAddLabel(accepted: List<Project>, verb: String = "Add"): String = when (accepted.size) {
    0 -> verb
    1 -> "$verb ${accepted.single().shortDisplayName()}"
    else -> "$verb ${accepted.size} projects"
}

fun primaryRemoveLabel(accepted: List<Project>): String = when (accepted.size) {
    0 -> "Remove"
    1 -> "Remove ${accepted.single().shortDisplayName()}"
    else -> "Remove ${accepted.size} projects"
}
