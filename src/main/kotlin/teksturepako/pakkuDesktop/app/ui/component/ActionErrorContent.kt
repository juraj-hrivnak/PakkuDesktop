/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.errors.CouldNotExport
import teksturepako.pakku.api.actions.errors.CouldNotImport
import teksturepako.pakku.api.actions.errors.CouldNotRead
import teksturepako.pakku.api.actions.errors.CouldNotSave
import teksturepako.pakku.api.actions.errors.DirectoryNotEmpty
import teksturepako.pakku.api.actions.errors.DownloadFailed
import teksturepako.pakku.api.actions.errors.ErrorSeverity
import teksturepako.pakku.api.actions.errors.ErrorWhileReading
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.actions.errors.HashMismatch
import teksturepako.pakku.api.actions.errors.MultipleErrors
import teksturepako.pakku.api.actions.errors.NoFiles
import teksturepako.pakku.api.actions.errors.NoFilesOn
import teksturepako.pakku.api.actions.errors.NoHashes
import teksturepako.pakku.api.actions.errors.NoUrl
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.actions.errors.NotRedistributable
import teksturepako.pakku.api.actions.errors.ProjDiffPLinks
import teksturepako.pakku.api.actions.errors.ProjDiffTypes
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.actions.errors.ProjRequiredBy
import teksturepako.pakku.api.actions.errors.VersionsDoNotMatch
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef

/**
 * GUI renderer for [ActionError]: maps every known error and embeds [ProjectRef]
 * (CF / MR / GH logos) for any project involved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionErrorContent(
    error: ActionError,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val color = when (error.severity) {
        ErrorSeverity.NOTICE -> PakkuDesktopConstants.amber.copy(alpha = 0.9f)
        ErrorSeverity.WARNING -> PakkuDesktopConstants.amber
        else -> PakkuDesktopConstants.coral
    }
    val fontSize = if (compact) 11.sp else 12.sp
    val iconSize = if (compact) 14.dp else 16.dp

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (error) {
            is MultipleErrors -> error.errors.forEach {
                ActionErrorContent(it, compact = compact)
            }

            is AlreadyAdded -> Phrase(color, fontSize) {
                Ref(error.project, fontSize, iconSize)
                T(" is already added.", color, fontSize)
            }

            is NotFoundOn -> Phrase(color, fontSize) {
                Ref(error.project, fontSize, iconSize)
                T(" was not found on ${error.provider.name}.", color, fontSize)
            }

            is NoFilesOn -> Phrase(color, fontSize) {
                T("No files for ", color, fontSize)
                Ref(error.project, fontSize, iconSize)
                T(" on ${error.provider.name}.", color, fontSize)
            }

            is NoFiles -> {
                Phrase(color, fontSize) {
                    T("No files found for ", color, fontSize)
                    Ref(error.project, fontSize, iconSize)
                    T(".", color, fontSize)
                }
                if (!compact) {
                    T(
                        "Requires Minecraft ${error.lockFile.getMcVersions()} / loaders ${error.lockFile.getLoaders()}.",
                        color.copy(alpha = 0.85f),
                        fontSize,
                    )
                }
            }

            is VersionsDoNotMatch -> {
                Phrase(color, fontSize) {
                    Ref(error.project, fontSize, iconSize)
                    T(" versions do not match across platforms.", color, fontSize)
                }
                if (!compact) {
                    T(
                        error.project.files.joinToString { "${it.type}: ${it.fileName}" },
                        color.copy(alpha = 0.85f),
                        fontSize,
                    )
                }
            }

            is ProjRequiredBy -> Phrase(color, fontSize) {
                Ref(error.project, fontSize, iconSize)
                T(" is required by ", color, fontSize)
                error.dependants.forEachIndexed { i, dep ->
                    if (i > 0) T(", ", color, fontSize)
                    Ref(dep, fontSize, iconSize)
                }
                T(".", color, fontSize)
            }

            is ProjNotFound -> {
                val proj = error.project
                if (proj != null) {
                    Phrase(color, fontSize) {
                        T("Project ", color, fontSize)
                        Ref(proj, fontSize, iconSize)
                        T(" not found.", color, fontSize)
                    }
                } else {
                    T(
                        if (!error.projectInput.isNullOrEmpty()) "Project '${error.projectInput}' not found."
                        else "Project not found.",
                        color,
                        fontSize,
                    )
                }
            }

            is ProjDiffTypes -> {
                T("Can not combine projects of different types:", color, fontSize)
                Phrase(color, fontSize) {
                    Ref(error.project, fontSize, iconSize)
                    T(" + ", color, fontSize)
                    Ref(error.otherProject, fontSize, iconSize)
                }
            }

            is ProjDiffPLinks -> {
                T("Can not combine projects with different pakku links:", color, fontSize)
                Phrase(color, fontSize) {
                    Ref(error.project, fontSize, iconSize)
                    T(" + ", color, fontSize)
                    Ref(error.otherProject, fontSize, iconSize)
                }
            }

            is NotRedistributable -> Phrase(color, fontSize) {
                Ref(error.project, fontSize, iconSize)
                T(" can not be exported (not redistributable).", color, fontSize)
            }

            is CouldNotExport -> T(
                "Profile ${error.profile.name} ('${error.modpackFileName}') could not be exported. ${error.reason.orEmpty()}".trimEnd(),
                color,
                fontSize,
            )

            is DirectoryNotEmpty -> T("Directory '${error.file}' is not empty.", color, fontSize)
            is FileNotFound -> T("File '${error.file}' not found.", color, fontSize)
            is CouldNotRead -> T("Could not read: '${error.file}'. ${error.reason.orEmpty()}".trimEnd(), color, fontSize)
            is ErrorWhileReading -> T("Error reading: '${error.file}'. ${error.reason.orEmpty()}".trimEnd(), color, fontSize)
            is AlreadyExists -> T("File '${error.file}' already exists.", color, fontSize)
            is CouldNotImport -> T("Could not import from: '${error.file}'.", color, fontSize)
            is NoUrl -> T("${error.projectFile.fileName} has no URL.", color, fontSize)
            is DownloadFailed -> T(
                "Failed to download '${error.path}'.${if (error.retryNumber > 0) " Retry ${error.retryNumber}." else ""}",
                color,
                fontSize,
            )
            is NoHashes -> T("File '${error.path}' has no hashes.", color, fontSize)
            is HashMismatch -> T("Hash mismatch for '${error.path}'.", color, fontSize)
            is CouldNotSave -> T(
                if (error.path != null) "Could not save: '${error.path}'. ${error.reason.orEmpty()}".trimEnd()
                else "Could not save file. ${error.reason.orEmpty()}".trimEnd(),
                color,
                fontSize,
            )

            else -> T(error.rawMessage, color, fontSize)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Phrase(
    color: Color,
    fontSize: TextUnit,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        content = { content() },
    )
}

@Composable
private fun Ref(
    project: Project,
    fontSize: TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    ProjectRef(project = project, fontSize = fontSize, iconSize = iconSize)
}

@Composable
private fun T(text: String, color: Color, fontSize: TextUnit) {
    Text(text = text, color = color, fontSize = fontSize)
}
