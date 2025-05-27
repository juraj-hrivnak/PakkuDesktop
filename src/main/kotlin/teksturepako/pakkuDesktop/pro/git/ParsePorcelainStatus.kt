/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import java.io.File

fun parsePorcelainStatus(lines: List<String>): List<GitFile>
{
    data class StatusMapping(
        val pattern: (Pair<Char, Char>) -> Boolean,
        val createChange: (String) -> GitChange,
    )

    val statusMappings = listOf(
        // Untracked files
        StatusMapping(
            pattern = { (x, y) -> x == '?' && y == '?' },
            createChange = { GitChange.Untracked(it) },
        ),
        // Added to index
        StatusMapping(
            pattern = { (x, y) -> x == 'A' || y == 'A' },
            createChange = { GitChange.Added(it) },
        ),
        // Deleted from index
        StatusMapping(
            pattern = { (x, y) -> x == 'D' || y == 'D' },
            createChange = { GitChange.Deleted(it) },
        ),
        // Modified in index or working tree
        StatusMapping(
            pattern = { (x, y) -> x == 'M' || y == 'M' },
            createChange = { GitChange.Modified(it) },
        ),
        // Renamed in index or working tree
        StatusMapping(
            pattern = { (x, y) -> x == 'R' || y == 'R' },
            createChange = { GitChange.Modified(it) },
        ),
        // Copied in index or working tree
        StatusMapping(
            pattern = { (x, y) -> x == 'C' || y == 'C' },
            createChange = { GitChange.Added(it) },
        ),
        // Updated but unmerged
        StatusMapping(
            pattern = { (x, y) -> x == 'U' || y == 'U' },
            createChange = { GitChange.Modified(it) },
        ),
    )

    return lines.mapNotNull { line ->
        if (line.isBlank())
        {
            return@mapNotNull null
        }

        val xy = line.substring(0, 2).let { status ->
            Pair(status[0], status[1])
        }

        val rawPath = line.drop(3)
        val path = if (rawPath.startsWith("\""))
        {
            unescapeOctal(rawPath)
                .removePrefix("\"")
                .removeSuffix("\"")
        }
        else
        {
            rawPath
        }

        statusMappings.find { it.pattern(xy) }?.let { mapping ->
            createGitFile(
                path = path,
                status = mapping.createChange(path),
            )
        }
    }
}

private fun unescapeOctal(input: String): String
{
    val regex = Regex("""\\([0-3]?[0-7]{1,2})""")
        .replace(input) { matchResult ->
            val octalValue = matchResult.groupValues[1]
            val charCode = Integer.parseInt(octalValue, 8)
            charCode.toChar().toString()
        }

    return String(
        regex.toByteArray(Charsets.ISO_8859_1),
        Charsets.UTF_8,
    )
}

private fun createGitFile(
    path: String,
    status: GitChange,
): GitFile
{
    val file = File(workingPath).resolve(path)
    return GitFile.fromFileInfo(
        path = path,
        status = status,
        modifiedEpochMillis = file.lastModified(),
        size = file.length(),
    )
}
