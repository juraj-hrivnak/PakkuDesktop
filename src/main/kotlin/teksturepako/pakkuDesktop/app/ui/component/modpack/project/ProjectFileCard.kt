/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.badgeStyle
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover
import teksturepako.pakkuDesktop.app.ui.model.ProjectFileChange

@Composable
fun ProjectFileCard(
    project: Project,
    projectFile: ProjectFile,
) {
    val latest = project.getLatestFile(project.getProviders().ifEmpty { Provider.providers })
    val isLatest = latest != null && projectFile == latest

    FileCardShell(
        borderColor = if (isLatest) {
            PakkuDesktopConstants.highlightColor.copy(alpha = 0.55f)
        } else {
            Color.Gray.copy(alpha = 0.3f)
        },
    ) {
        FileCardBody(
            projectFile = projectFile,
            label = if (isLatest) "current" else null,
        )
    }
}

/**
 * Compact old → new update preview; extra candidates are a clickable list under the selected file.
 */
@Composable
fun ProjectFileUpdateCard(
    change: ProjectFileChange,
    onSelectFile: (fileId: String) -> Unit,
) {
    val removed = diffAccent(removed = true)
    val added = diffAccent(removed = false)
    val selected = change.selectedFile
    val others = change.newFiles.filter { it.fileKey() != selected.fileKey() }

    FileCardShell(borderColor = JewelTheme.globalColors.borders.normal) {
        DiffFileRow(
            prefix = "−",
            accent = removed,
            projectFile = change.oldFile,
            label = null,
            showHashes = true,
        )
        DiffFileRow(
            prefix = "+",
            accent = added,
            projectFile = selected,
            label = "selected",
            showHashes = true,
        )

        if (others.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickableHover(scaleOnHover = false) { expanded = !expanded }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    key = if (expanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.Gray.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Other versions (${others.size})",
                    color = Color.Gray.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                )
            }
            if (expanded) {
                others.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickableHover(scaleOnHover = false) { onSelectFile(file.fileKey()) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProviderTypeIcon(file.type)
                        Column(modifier = Modifier.weight(1f)) {
                            SelectableText(file.fileName, fontSize = 12.sp)
                            file.datePublished
                                .takeUnless { it == Instant.DISTANT_PAST }
                                ?.formatPublished()
                                ?.let {
                                    Text(
                                        text = it,
                                        color = Color.Gray.copy(alpha = 0.65f),
                                        fontSize = 11.sp,
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

private fun ProjectFile.fileKey(): String = id.ifEmpty { fileName }
@Composable
private fun diffAccent(removed: Boolean): Color {
    val palette = JewelTheme.colorPalette
    return if (removed) {
        palette.red.getOrNull(palette.red.lastIndex / 2) ?: Color(0xFFE05252)
    } else {
        palette.green.getOrNull(palette.green.lastIndex / 2) ?: Color(0xFF3DDC97)
    }
}

@Composable
private fun DiffFileRow(
    prefix: String,
    accent: Color,
    projectFile: ProjectFile,
    label: String?,
    showHashes: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = prefix,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            FileCardBody(
                projectFile = projectFile,
                label = label,
                showHashes = showHashes,
            )
        }
    }
}

@Composable
private fun FileCardShell(
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun FileCardBody(
    projectFile: ProjectFile,
    label: String?,
    showHashes: Boolean = true,
) {
    val published = projectFile.datePublished
        .takeUnless { it == Instant.DISTANT_PAST }
        ?.formatPublished()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            ProviderTypeIcon(projectFile.type)

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectableText(
                        text = projectFile.fileName,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (label != null) {
                        Badge(style = JewelTheme.badgeStyle.blueSecondary) {
                            Text(label)
                        }
                    }
                }
                published?.let {
                    Text(
                        text = it,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        CopyToClipboardButton(
            text = projectFile.fileName,
            modifier = Modifier.size(25.dp),
        )
    }

    val hashes = projectFile.hashes
    if (showHashes && !hashes.isNullOrEmpty()) {
        Column(
            modifier = Modifier.padding(start = 29.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            hashes.entries
                .sortedBy { it.key.lowercase() }
                .forEach { (alg, hash) ->
                    HashRow(algorithm = alg, hash = hash)
                }
        }
    }
}

@Composable
private fun HashRow(
    algorithm: String,
    hash: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = algorithm.lowercase(),
            color = Color.Gray.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.widthIn(min = 52.dp),
        )
        SelectableText(
            text = hash,
            color = JewelTheme.contentColor.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        CopyToClipboardButton(
            text = hash,
            modifier = Modifier.size(22.dp),
            useSimpleTooltip = true,
        )
    }
}

@Composable
private fun ProviderTypeIcon(type: String) {
    when (type) {
        "curseforge" -> Icon(
            PakkuDesktopIcons.Platforms.curseForge,
            type,
            modifier = Modifier.size(25.dp),
        )
        "github" -> Icon(
            PakkuDesktopIcons.Platforms.gitHub,
            type,
            modifier = Modifier.size(25.dp),
            tint = JewelTheme.contentColor,
        )
        "modrinth" -> Icon(
            PakkuDesktopIcons.Platforms.modrinth,
            type,
            modifier = Modifier.size(25.dp),
        )
        else -> Text(type)
    }
}

internal fun Instant.formatPublished(): String =
    format(DateTimeComponents.Formats.RFC_1123)
