/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

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
import teksturepako.pakku.api.http.ConnectionError
import teksturepako.pakku.api.http.RequestError

/** Host-owned stand-in for a Pakku error that would otherwise pin a [Throwable] or an HTTP response. */
private class DetachedActionError(
    override val rawMessage: String,
    override val severity: ErrorSeverity,
) : ActionError()

/**
 * Copy safe to keep in the model: [ConnectionError] (holds the exception) and [RequestError]
 * (holds the `HttpResponse`) are replaced by their message and severity.
 */
fun ActionError.detached(): ActionError = when (this) {
    is ConnectionError, is RequestError -> DetachedActionError(rawMessage, severity)
    is MultipleErrors -> MultipleErrors(*errors.map { it.detached() }.toTypedArray())
    else -> this
}

/**
 * Plain-text fallback for toasts / logs. GUI should use [teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent].
 */
fun ActionError.toUiMessage(arg: String = ""): String = when (this) {
    is MultipleErrors -> errors.joinToString("\n") { it.toUiMessage(arg) }
    is DirectoryNotEmpty -> "Directory '$file' is not empty."
    is FileNotFound -> "File '$file' not found."
    is CouldNotRead -> "Could not read: '$file'. ${reason.orEmpty()}".trimEnd()
    is ErrorWhileReading -> "Error occurred while reading: '$file'. ${reason.orEmpty()}".trimEnd()
    is AlreadyExists -> "File '$file' already exists."
    is NoUrl -> "${projectFile.fileName} has no URL."
    is DownloadFailed ->
        "Failed to download '$path'.${if (retryNumber > 0) " Retry number $retryNumber." else ""}"
    is NoHashes -> "File '$path' has no hashes."
    is HashMismatch -> "Hash mismatch for '$path'."
    is CouldNotSave ->
        if (path != null) "Could not save: '$path'. ${reason.orEmpty()}".trimEnd()
        else "Could not save file. ${reason.orEmpty()}".trimEnd()
    is CouldNotImport -> "Could not import from: '$file'."
    is ProjNotFound -> {
        val proj = project
        when {
            proj != null -> "Project ${proj.displayLabel()} not found."
            !projectInput.isNullOrEmpty() -> "Project '$projectInput' not found."
            else -> "Project not found."
        }
    }
    is ProjDiffTypes ->
        "Can not combine projects of different types: ${project.displayLabel()} + ${otherProject.displayLabel()}"
    is ProjDiffPLinks ->
        "Can not combine projects with different pakku links: ${project.displayLabel()} + ${otherProject.displayLabel()}"
    is NotRedistributable ->
        "${project.displayLabel()} can not be exported, because it is not redistributable."
    is CouldNotExport ->
        "Profile ${profile.name} ('$modpackFileName') could not be exported. ${reason.orEmpty()}".trimEnd()
    is AlreadyAdded -> "${project.displayLabel()} is already added."
    is NotFoundOn -> "${project.displayLabel()} was not found on ${provider.name}."
    is NoFilesOn -> "No files for ${project.displayLabel()} found on ${provider.name}."
    is NoFiles ->
        "No files found for ${project.displayLabel()}. Requires Minecraft ${lockFile.getMcVersions()} / loaders ${lockFile.getLoaders()}."
    is VersionsDoNotMatch ->
        "${project.displayLabel()} versions do not match across platforms."
    is ProjRequiredBy -> {
        "${project.displayLabel()} is required by ${dependants.toListMsg { it.displayLabel() }}."
    }
    else -> rawMessage
}

/** Same join as Pakku [teksturepako.pakku.cli.ui.toMsg]: `a, b & c`, with the comma on the previous item. */
private fun <T> List<T>.toListMsg(label: (T) -> String): String = when (size) {
    0 -> ""
    1 -> label(first())
    else -> foldIndexed("") { i, acc, value ->
        when {
            acc.isEmpty() -> label(value)
            i == lastIndex -> "$acc & ${label(value)}"
            else -> "$acc, ${label(value)}"
        }
    }
}
