/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.badgeStyle
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkuDesktop.app.integration.revealInFileManager
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.statusColor
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.ProjectFileChange
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import java.nio.file.Path
import kotlin.io.path.exists

@Composable
fun ProjectFileCard(
    project: Project,
    projectFile: ProjectFile,
    configFile: ConfigFile? = null,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    checked: Boolean = false,
    mergedFiles: List<ProjectFile> = emptyList(),
    showLatestLabel: Boolean = true,
    syncKind: SyncKind? = null,
    changeKind: FilesChangeKind? = null,
    pathOverride: String? = null,
) {
    val latest = project.getLatestFile(project.getProviders().ifEmpty { Provider.providers })
    val isLatest = latest != null && projectFile == latest
    val relativePath = pathOverride ?: projectFile.getRelativePathString(project, configFile)
    val diskPath = remember(project, projectFile, configFile) {
        projectFile.getPath(project, configFile)
    }
    val canOpenLocation = diskPath != null && diskPath.exists()
    val providerTypes = remember(projectFile, mergedFiles) {
        (listOf(projectFile) + mergedFiles).map { it.type }.distinct()
    }
    val kind = changeKind ?: syncKind?.toFilesChangeKind()

    val borderColor = fileCardBorder(focused, checked, kind, isLatest && showLatestLabel)
    val fill = fileCardFill(focused, checked, kind)

    FileCardShell(
        borderColor = borderColor,
        modifier = modifier,
        fill = fill,
        borderWidth = if (focused || checked || kind != null) 1.5.dp else 1.dp,
    ) {
        FileCardBody(
            projectFile = projectFile,
            label = if (showLatestLabel && isLatest) "current" else null,
            relativePath = relativePath,
            openInFolderPath = if (canOpenLocation) diskPath else null,
            changeKind = kind,
            providerTypes = providerTypes,
        )
    }
}

@Composable
fun ProjectFileCard(
    relativePath: String,
    changeKind: FilesChangeKind?,
    checked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderColor = fileCardBorder(focused = false, checked = checked, kind = changeKind, latest = false)
    val fill = fileCardFill(focused = false, checked = checked, kind = changeKind)
    FileCardShell(
        borderColor = borderColor,
        modifier = modifier,
        fill = fill,
        borderWidth = if (checked || changeKind != null) 1.5.dp else 1.dp,
    ) {
        FileCardBody(
            projectFile = null,
            label = null,
            showHashes = false,
            relativePath = relativePath,
            changeKind = changeKind,
            providerTypes = emptyList(),
        )
    }
}

private fun SyncKind.toFilesChangeKind(): FilesChangeKind = when (this) {
    SyncKind.Added -> FilesChangeKind.Added
    SyncKind.Removed -> FilesChangeKind.Removed
    SyncKind.Updated -> FilesChangeKind.Updated
}

@Composable
private fun fileCardBorder(
    focused: Boolean,
    checked: Boolean,
    kind: FilesChangeKind?,
    latest: Boolean,
): Color = when {
    focused -> PakkuDesktopConstants.highlightColor
    checked -> JewelTheme.contentColor.copy(alpha = 0.42f)
    kind != null -> kind.accentColor().copy(alpha = 0.9f)
    latest -> PakkuDesktopConstants.highlightColor.copy(alpha = 0.55f)
    else -> Color.Gray.copy(alpha = 0.3f)
}

@Composable
private fun fileCardFill(
    focused: Boolean,
    checked: Boolean,
    kind: FilesChangeKind?,
): Color? = when {
    focused -> PakkuDesktopConstants.highlightColor.copy(alpha = 0.12f)
    checked -> JewelTheme.contentColor.copy(alpha = 0.07f)
    kind != null -> kind.accentColor().copy(alpha = 0.10f)
    else -> null
}

@Composable
private fun FilesChangeKind.accentColor(): Color = statusColor()

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
    modifier: Modifier = Modifier,
    fill: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (fill != null) Modifier.background(fill) else Modifier)
            .border(
                width = borderWidth,
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
    projectFile: ProjectFile?,
    label: String?,
    showHashes: Boolean = true,
    relativePath: String? = null,
    openInFolderPath: Path? = null,
    changeKind: FilesChangeKind? = null,
    providerTypes: List<String> = projectFile?.let { listOf(it.type) }.orEmpty(),
) {
    val published = projectFile?.datePublished
        ?.takeUnless { it == Instant.DISTANT_PAST }
        ?.formatPublished()
    val pathText = relativePath ?: projectFile?.fileName.orEmpty()
    val codeBg = JewelTheme.globalColors.borders.disabled.copy(alpha = 0.45f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            providerTypes.forEach { type ->
                ProviderTypeIcon(type)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(4.dp))
                        .background(codeBg)
                        .border(
                            width = 1.dp,
                            color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    SelectableText(
                        text = pathText,
                        color = JewelTheme.contentColor.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CopyToClipboardButton(text = pathText)
                if (openInFolderPath != null) {
                    PkUiTooltip(tooltip = { Text("Show in folder") }) {
                        IconButton(
                            onClick = { revealInFileManager(openInFolderPath) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                key = AllIconsKeys.General.OpenDisk,
                                contentDescription = "Show in folder",
                                tint = Color.Gray,
                                hints = arrayOf(),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                if (label != null) {
                    Badge(style = JewelTheme.badgeStyle.blueSecondary) {
                        Text(label)
                    }
                }
                when (changeKind) {
                    FilesChangeKind.Fetch -> Badge(style = JewelTheme.badgeStyle.green) {
                        Text("Download")
                    }
                    FilesChangeKind.Override -> Badge(style = JewelTheme.badgeStyle.green) {
                        Text("Sync")
                    }
                    FilesChangeKind.Cleanup -> Badge(style = JewelTheme.badgeStyle.graySecondary) {
                        Text("Delete")
                    }
                    FilesChangeKind.Added -> Badge(style = JewelTheme.badgeStyle.green) {
                        Text("Add")
                    }
                    FilesChangeKind.Removed -> Badge(style = JewelTheme.badgeStyle.graySecondary) {
                        Text("Remove")
                    }
                    FilesChangeKind.Updated -> Badge(style = JewelTheme.badgeStyle.blueSecondary) {
                        Text("Update")
                    }
                    FilesChangeKind.HashMismatch -> Badge(style = JewelTheme.badgeStyle.blueSecondary) {
                        Text("Hash mismatch")
                    }
                    null -> Unit
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

    if (showHashes) {
        val sha1 = projectFile?.hashes
            ?.entries
            ?.firstOrNull { it.key.equals("sha1", ignoreCase = true) }
            ?.value
        if (!sha1.isNullOrBlank()) {
            HashRow(hash = sha1)
        }
    }
}

@Composable
private fun HashRow(hash: String) {
    val short = shortHash(hash)
    val codeBg = JewelTheme.globalColors.borders.disabled.copy(alpha = 0.45f)

    Row(
        modifier = Modifier.padding(start = 29.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "sha1",
            color = Color.Gray.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(codeBg)
                .border(
                    width = 1.dp,
                    color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            SelectableText(
                text = short,
                color = JewelTheme.contentColor.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CopyToClipboardButton(
            text = hash,
        )
    }
}

/** GitHub-style abbreviated hash for display; copy still uses the full value. */
private fun shortHash(hash: String, length: Int = 7): String =
    if (hash.length <= length) hash else hash.take(length)

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
