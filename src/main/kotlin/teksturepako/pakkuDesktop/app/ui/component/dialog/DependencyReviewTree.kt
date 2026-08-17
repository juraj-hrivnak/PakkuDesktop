/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
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
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef

/** Width of the checkbox column — deps align to the content column beside it. */
private val CheckboxCol = 28.dp

/** Width of one tree-guide column (continuous ancestor stems + elbow). */
private val GuideCol = 14.dp

/** Stroke for Pakku-like tree guides. */
private val GuideStroke = 1.5.dp

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
                ProjectRef(replacing)
                Text("→", color = JewelTheme.contentColor.copy(alpha = 0.45f), fontSize = 12.sp)
                ProjectRef(project)
            } else {
                ProjectRef(project)
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
            ProjectRef(project)
        }

        ContentUnderRoot {
            warning?.let { ContextualWarning(it, shownFingerprints) }
            if (checked && orphanedChildren.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionLabel(depSectionLabel)
                    orphanedChildren.forEachIndexed { index, child ->
                        val isLast = index == orphanedChildren.lastIndex
                        RemovableDepRow(
                            entry = child,
                            checked = child.key in acceptedDepIds,
                            onCheckedChange = { on -> onDepCheckedChange(child.key, on) },
                            isLast = isLast,
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
    isLast: Boolean,
    shownFingerprints: MutableSet<String>,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TreeElbow(isLast = isLast, hasChildren = false)
            Box(
                Modifier.width(CheckboxCol),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
            ProjectRef(entry.project, fontSize = 12.sp, iconSize = 14.dp)
            StatusChip(
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
                Modifier.padding(start = GuideCol + CheckboxCol + 6.dp),
            )
        }
    }
}

@Composable
private fun RootCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
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
        SectionLabel(label)
        DepTree(
            nodes = nodes,
            depth = 0,
            ancestorContinues = emptyList(),
            cue = cue,
            shownFingerprints = shownFingerprints,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = JewelTheme.contentColor.copy(alpha = 0.5f),
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
    )
}

@Composable
private fun DepTree(
    nodes: List<DepNode>,
    depth: Int,
    ancestorContinues: List<Boolean>,
    cue: String,
    shownFingerprints: MutableSet<String>,
) {
    nodes.forEachIndexed { index, node ->
        val isLast = index == nodes.lastIndex
        DepLeafRow(
            project = node.project,
            depth = depth,
            isLast = isLast,
            ancestorContinues = ancestorContinues,
            alreadyPresent = node.alreadyPresent,
            cue = cue,
            hasChildren = node.children.isNotEmpty(),
        )
        node.warnings.forEach {
            ContextualWarning(
                it,
                shownFingerprints,
                Modifier.padding(start = GuideCol * (depth + 1) + 10.dp),
            )
        }
        if (node.children.isNotEmpty()) {
            DepTree(
                nodes = node.children,
                depth = depth + 1,
                ancestorContinues = ancestorContinues + !isLast,
                cue = cue,
                shownFingerprints = shownFingerprints,
            )
        }
    }
}

@Composable
private fun DepLeafRow(
    project: Project,
    depth: Int,
    isLast: Boolean,
    ancestorContinues: List<Boolean>,
    alreadyPresent: Boolean,
    cue: String,
    hasChildren: Boolean,
) {
    val rowHeight = 24.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TreeGuides(
            depth = depth,
            isLast = isLast,
            ancestorContinues = ancestorContinues,
            hasChildren = hasChildren,
            rowHeight = rowHeight,
            accent = if (alreadyPresent) {
                JewelTheme.contentColor.copy(alpha = 0.28f)
            } else {
                PakkuDesktopConstants.highlightColor.copy(alpha = 0.55f)
            },
        )
        ProjectRef(project, fontSize = 12.sp, iconSize = 14.dp)
        StatusChip(
            text = if (alreadyPresent) "in pack" else cue,
            muted = alreadyPresent,
        )
    }
}

/**
 * Continuous ancestor stems + elbow/T into the node, with a Pakku cyan joint.
 */
@Composable
private fun TreeGuides(
    depth: Int,
    isLast: Boolean,
    ancestorContinues: List<Boolean>,
    hasChildren: Boolean,
    rowHeight: Dp,
    accent: Color,
) {
    Row(Modifier.height(rowHeight)) {
        repeat(depth) { level ->
            val continues = ancestorContinues.getOrNull(level) == true
            Box(
                Modifier
                    .width(GuideCol)
                    .fillMaxHeight(),
            ) {
                if (continues) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .width(GuideStroke)
                            .fillMaxHeight()
                            .background(accent.copy(alpha = 0.35f)),
                    )
                }
            }
        }
        TreeElbow(
            isLast = isLast,
            hasChildren = hasChildren,
            accent = accent,
        )
    }
}

@Composable
private fun TreeElbow(
    isLast: Boolean,
    hasChildren: Boolean,
    accent: Color = PakkuDesktopConstants.highlightColor.copy(alpha = 0.55f),
) {
    Box(
        Modifier
            .width(GuideCol)
            .fillMaxHeight(),
    ) {
        // Vertical: full for T (more siblings below), half for L (last).
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(GuideStroke)
                .fillMaxHeight(if (isLast) 0.5f else 1f)
                .background(accent),
        )
        // Stem downward into children (keeps parent→child continuous).
        if (hasChildren) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(GuideStroke)
                    .fillMaxHeight(0.5f)
                    .background(accent),
            )
        }
        // Horizontal arm
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = GuideCol / 2)
                .width(GuideCol / 2)
                .height(GuideStroke)
                .background(accent),
        )
        // Joint
        Box(
            Modifier
                .align(Alignment.Center)
                .size(6.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.95f)),
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    muted: Boolean,
    accent: Color = PakkuDesktopConstants.highlightColor,
) {
    val bg = if (muted) {
        JewelTheme.globalColors.borders.normal.copy(alpha = 0.35f)
    } else {
        accent.copy(alpha = 0.18f)
    }
    val fg = if (muted) {
        JewelTheme.contentColor.copy(alpha = 0.55f)
    } else {
        accent.copy(alpha = 0.95f)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text, color = fg, fontSize = 10.sp)
    }
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
            Row(
                modifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Required by", color = muted, fontSize = 11.sp)
                error.dependants.forEachIndexed { i, dep ->
                    if (i > 0) Text(",", color = muted, fontSize = 11.sp)
                    ProjectRef(dep, fontSize = 11.sp, iconSize = 12.dp)
                }
            }
        }
        is NotFoundOn -> Text(
            "Not found on ${error.provider.name}",
            color = color,
            fontSize = 11.sp,
            modifier = modifier,
        )
        is NoFilesOn -> Text(
            "No files on ${error.provider.name}",
            color = color,
            fontSize = 11.sp,
            modifier = modifier,
        )
        is NoFiles -> Text(
            "No matching files for this pack",
            color = color,
            fontSize = 11.sp,
            modifier = modifier,
        )
        is VersionsDoNotMatch -> Text(
            "Versions do not match across platforms",
            color = color,
            fontSize = 11.sp,
            modifier = modifier,
        )
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
fun ReviewDialogFooter(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f)),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onSecondary) {
                Text(secondaryLabel)
            }
            DefaultButton(
                onClick = onPrimary,
                enabled = primaryEnabled,
            ) {
                Text(primaryLabel)
            }
        }
    }
}

fun Project.shortDisplayName(): String =
    name.values.firstOrNull()
        ?: slug.values.firstOrNull()
        ?: id.values.firstOrNull()
        ?: "project"

fun primaryAddLabel(accepted: List<Project>): String = when (accepted.size) {
    0 -> "Add"
    1 -> "Add ${accepted.single().shortDisplayName()}"
    else -> "Add ${accepted.size} projects"
}

fun primaryRemoveLabel(accepted: List<Project>): String = when (accepted.size) {
    0 -> "Remove"
    1 -> "Remove ${accepted.single().shortDisplayName()}"
    else -> "Remove ${accepted.size} projects"
}
