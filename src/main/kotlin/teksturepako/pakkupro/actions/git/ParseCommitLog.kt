package teksturepako.pakkupro.actions.git

import teksturepako.pakkupro.ui.viewmodel.state.GitCommit

fun parseCommitLog(input: List<String>): List<GitCommit>
{
    return input
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { line -> parseCommitLine(line) }
}

private fun parseCommitLine(line: String): GitCommit?
{
    val parts = line.split(" ", limit = 2)

    return when (parts.size)
    {
        2 ->
            GitCommit(
                hash = parts[0],
                message = parts[1],
            )
        1 ->
            GitCommit(
                hash = parts[0],
                message = "",
            )
        else -> null
    }
}