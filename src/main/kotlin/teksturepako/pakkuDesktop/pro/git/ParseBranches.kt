/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch

fun parseGitBranches(input: List<String>): List<GitBranch>
{
    return input
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { "->" in it }
        .map { line -> parseBranchLine(line) }
}

private fun parseBranchLine(line: String): GitBranch
{
    val isCurrent = line.startsWith("*")
    val cleanLine = line.removePrefix("*").trim()

    return when
    {
        cleanLine.startsWith("remotes/") -> parseRemoteBranch(cleanLine)
        else -> parseLocalBranch(cleanLine, isCurrent)
    }
}

private fun parseRemoteBranch(line: String): GitBranch
{
    val cleanLine = line.removePrefix("remotes/")

    return GitBranch(
        name = cleanLine,
        isRemote = true,
        isCurrent = false,
    )
}

private fun parseLocalBranch(line: String, isCurrent: Boolean): GitBranch
{
    return GitBranch(
        name = line,
        isRemote = false,
        isCurrent = isCurrent,
    )
}