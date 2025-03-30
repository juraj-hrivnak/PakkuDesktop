package teksturepako.pakkupro.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import teksturepako.pakkupro.ui.viewmodel.state.*
import java.nio.file.Path
import kotlin.math.absoluteValue

object GitViewModel
{
    private val _state = MutableStateFlow(GitState())
    val state: StateFlow<GitState> = _state.asStateFlow()

    private fun updateState(update: (GitState) -> GitState)
    {
        _state.update(update)
    }

    private fun longestCommonSubsequence(a: List<String>, b: List<String>): List<String>
    {
        val lengths = Array(a.size + 1) { IntArray(b.size + 1) }

        a.indices.fold(lengths) { acc, i ->
            b.indices.fold(acc) { innerAcc, j ->
                innerAcc.also {
                    it[i + 1][j + 1] = if (a[i] == b[j])
                    {
                        it[i][j] + 1
                    }
                    else
                    {
                        maxOf(it[i + 1][j], it[i][j + 1])
                    }
                }
            }
        }

        tailrec fun buildLCS(
            i: Int,
            j: Int,
            result: List<String>,
        ): List<String> = when
        {
            i <= 0 || j <= 0                      -> result
            a[i - 1] == b[j - 1]                  -> buildLCS(i - 1, j - 1, listOf(a[i - 1]) + result)
            lengths[i - 1][j] > lengths[i][j - 1] -> buildLCS(i - 1, j, result)
            else                                  -> buildLCS(i, j - 1, result)
        }

        return buildLCS(a.size, b.size, emptyList())
    }

    private fun <T> withGitState(block: (Git, Repository) -> T): T?
    {
        val currentState = _state.value
        val git = currentState.git
        val repository = currentState.repository
        return if (git != null && repository != null)
        {
            block(git, repository)
        }
        else null
    }

    private fun createRepository(path: Path): Result<Repository> = runCatching {
        FileRepositoryBuilder()
            .setGitDir(path.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build()
    }

    fun initialize(path: Path)
    {
        createRepository(path)
            .map { repository ->
                updateState { GitState.fromRepository(repository) }
                refreshChanges()
            }
            .onFailure { e ->
                println("Failed to initialize Git repository: ${e.message}")
                e.printStackTrace()
            }
    }

    private fun createGitFile(path: String, status: GitChange, workTree: java.io.File): GitFile =
        workTree.resolve(path).let { file ->
            GitFile.fromFileInfo(
                path = path,
                status = status,
                modifiedEpochMillis = file.lastModified(),
                size = file.length()
            )
        }

    private fun processGitStatus(status: Status, workTree: java.io.File): List<GitFile>
    {
        data class StatusMapping(
            val paths: Set<String>,
            val createChange: (String) -> GitChange
        )

        val statusMappings = listOf(
            StatusMapping(status.untracked) { GitChange.Untracked(it) },
            StatusMapping(status.modified) { GitChange.Modified(it) },
            StatusMapping(status.added) { GitChange.Added(it) },
            StatusMapping(status.changed) { GitChange.Modified(it) },
            StatusMapping(status.removed) { GitChange.Deleted(it) }
        )

        return statusMappings.flatMap { (paths, createChange) ->
            paths.map { path -> createGitFile(path, createChange(path), workTree) }
        }
    }

    private fun refreshChanges()
    {
        withGitState { git, repository ->
            runCatching {
                val files = processGitStatus(git.status().call(), repository.workTree)
                updateState { it.copy(gitFiles = files) }
            }.onFailure { e ->
                println("Error refreshing changes: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun toggleFileSelection(path: String) = updateState { currentState ->
        val newSelectedFiles = currentState.selectedFiles.let { selected ->
            if (selected.contains(path)) selected - path else setOf(path)
        }

        val newDiff = withGitState { git, repository ->
            runCatching {
                newSelectedFiles.singleOrNull()?.let { selectedPath ->
                    when {
                        git.status().call().untracked.contains(selectedPath) -> computeUntrackedDiff(selectedPath, repository)
                        else -> computeTrackedDiff(selectedPath, repository, git)
                    }
                }
            }.getOrNull()
        }

        currentState.copy(
            selectedFiles = newSelectedFiles,
            currentDiff = newDiff
        )
    }

    private fun computeUntrackedDiff(path: String, repository: Repository): DiffContent
    {
        val lines = repository.workTree.resolve(path).readText().lines()

        return DiffContent(
            oldPath = null,
            newPath = path,
            hunks = listOf(createDiffHunk(lines))
        )
    }

    private fun computeTrackedDiff(path: String, repository: Repository, git: Git): DiffContent
    {
        val file = repository.workTree.resolve(path)
        val currentContent = file.readText()
        val currentLines = currentContent.lines()

        // Get the previous version's content using functional approach
        val previousContent = getPreviousContent(path, repository, git)
        val previousLines = previousContent.lines()

        // Compare the lines using our existing utility function
        val diffLines = compareLines(previousLines, currentLines)

        // Convert the diff lines to hunks
        return createDiffContent(path, diffLines)
    }

    // Helper function to get previous content safely
    private fun getPreviousContent(path: String, repository: Repository, git: Git): String = runCatching {
        repository.resolve("HEAD")?.let { head ->
            val reader = repository.newObjectReader()
            val treeId = repository.resolve("HEAD^{tree}")

            val oldTreeParser = CanonicalTreeParser().apply {
                reset(reader, treeId)
            }

            // Get current tree
            val newTreeParser = FileTreeIterator(repository)

            // Get diff entries
            git.diff()
                .setOldTree(oldTreeParser)
                .setNewTree(newTreeParser)
                .setPathFilter(PathFilter.create(path))
                .call()
                .firstOrNull()
                ?.let { diff ->
                    val objectId = diff.oldId.toObjectId()
                    repository.open(objectId).bytes.toString(Charsets.UTF_8)
                }
        }
    }.getOrNull() ?: ""

    // Helper function to create DiffContent from diff lines
    private fun createDiffContent(path: String, diffLines: List<Pair<DiffType, String>>): DiffContent
    {
        val hunks = mutableListOf<DiffHunk>()
        val currentHunkLines = mutableListOf<DiffLine>()
        var oldLineNum = 1
        var newLineNum = 1
        var hunkStart = true

        fun addCurrentHunk()
        {
            if (currentHunkLines.isNotEmpty())
            {
                val oldCount = currentHunkLines.count { it.number.old != null }
                val newCount = currentHunkLines.count { it.number.new != null }
                val hunkHeader = "@@ -${oldLineNum - oldCount},$oldCount +${newLineNum - newCount},$newCount @@"

                hunks.add(DiffHunk(
                    header = hunkHeader,
                    lines = currentHunkLines.toList()
                ))
                currentHunkLines.clear()
                hunkStart = true
            }
        }

        diffLines.forEach { (type, line) ->
            when (type)
            {
                DiffType.ADDED ->
                {
                    if (hunkStart) addCurrentHunk()
                    hunkStart = false
                    currentHunkLines.add(DiffLine(
                        number = DiffLine.LineNumbers(null, newLineNum++),
                        content = line,
                        type = DiffType.ADDED
                    ))
                }
                DiffType.DELETED ->
                {
                    if (hunkStart) addCurrentHunk()
                    hunkStart = false
                    currentHunkLines.add(DiffLine(
                        number = DiffLine.LineNumbers(oldLineNum++, null),
                        content = line,
                        type = DiffType.DELETED
                    ))
                }
                DiffType.UNCHANGED ->
                {
                    currentHunkLines.add(DiffLine(
                        number = DiffLine.LineNumbers(oldLineNum++, newLineNum++),
                        content = line,
                        type = DiffType.UNCHANGED
                    ))
                }
            }
        }

        // Add the last hunk
        addCurrentHunk()

        return DiffContent(
            oldPath = path,
            newPath = path,
            hunks = hunks
        )
    }

    // Utility function for creating diff hunks
    private fun createDiffHunk(lines: List<String>): DiffHunk =
        DiffHunk(
            header = "@@ -0,0 +1,${lines.size} @@",
            lines = lines.mapIndexed { index, line ->
                DiffLine(
                    number = DiffLine.LineNumbers(null, index + 1),
                    content = line,
                    type = DiffType.ADDED
                )
            }
        )

    fun updateCommitMessage(message: String)
    {
        updateState { it.copy(commitMessage = message) }
    }

    fun commit() {
        val currentState = _state.value
        val message = currentState.commitMessage.takeIf { it.isNotBlank() } ?: return

        withGitState { git, _ ->
            runCatching {
                git.add().addFilepattern(".").call()
                git.commit().setMessage(message).call()

                updateState { it.copy(commitMessage = "") }
                refreshChanges()
            }.onFailure { e ->
                println("Error committing changes: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun compareLines(oldLines: List<String>, newLines: List<String>): List<Pair<DiffType, String>>
    {
        val lcs = longestCommonSubsequence(oldLines, newLines)
        return buildDiffSequence(oldLines, newLines, lcs).toList()
    }

    private fun buildDiffSequence(
        oldLines: List<String>,
        newLines: List<String>,
        lcs: List<String>
    ): Sequence<Pair<DiffType, String>> = sequence {
        var (oldIndex, newIndex, lcsIndex) = Triple(0, 0, 0)

        while (oldIndex < oldLines.size || newIndex < newLines.size)
        {
            when
            {
                isMatchingLine(oldIndex, newIndex, lcsIndex, oldLines, newLines, lcs) ->
                {
                    yield(DiffType.UNCHANGED to oldLines[oldIndex])
                    oldIndex++; newIndex++; lcsIndex++
                }

                canDeleteOldLine(oldIndex, lcsIndex, oldLines, lcs)
                        && canAddNewLine(newIndex, lcsIndex, newLines, lcs)
                        && (oldLines[oldIndex].length - newLines[newIndex].length).absoluteValue < 10 ->
                {
                    // Consider it a modification if the line lengths are similar
                    yield(DiffType.ADDED to newLines[newIndex])
                    yield(DiffType.DELETED to oldLines[oldIndex])
                    oldIndex++
                    newIndex++
                }

                canAddNewLine(newIndex, lcsIndex, newLines, lcs) ->
                {
                    yield(DiffType.ADDED to newLines[newIndex])
                    newIndex++
                }

                canDeleteOldLine(oldIndex, lcsIndex, oldLines, lcs) ->
                {
                    yield(DiffType.DELETED to oldLines[oldIndex])
                    oldIndex++
                }

                else -> break
            }
        }
    }

    // Helper functions for diff comparison
    private fun isMatchingLine(
        oldIndex: Int,
        newIndex: Int,
        lcsIndex: Int,
        oldLines: List<String>,
        newLines: List<String>,
        lcs: List<String>
    ): Boolean = lcsIndex < lcs.size
            && oldIndex < oldLines.size
            && newIndex < newLines.size
            && oldLines[oldIndex] == lcs[lcsIndex]
            && newLines[newIndex] == lcs[lcsIndex]

    private fun canAddNewLine(
        newIndex: Int,
        lcsIndex: Int,
        newLines: List<String>,
        lcs: List<String>
    ): Boolean = newIndex < newLines.size
            && (lcsIndex >= lcs.size || newLines[newIndex] != lcs[lcsIndex])

    private fun canDeleteOldLine(
        oldIndex: Int,
        lcsIndex: Int,
        oldLines: List<String>,
        lcs: List<String>
    ): Boolean = oldIndex < oldLines.size
            && (lcsIndex >= lcs.size || oldLines[oldIndex] != lcs[lcsIndex])
}