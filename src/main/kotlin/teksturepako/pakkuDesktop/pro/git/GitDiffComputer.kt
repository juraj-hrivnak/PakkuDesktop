/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffHunk
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffLine
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import java.nio.file.Path
import kotlin.math.absoluteValue

object GitDiffComputer {

    fun openRepository(path: Path): Result<Pair<Git, Repository>> = runCatching {
        val repository = FileRepositoryBuilder()
            .setGitDir(path.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build()
        Git(repository) to repository
    }

    fun computeDiff(git: Git, repository: Repository, gitFile: GitFile): DiffContent? = runCatching {
        when {
            git.status().call().untracked.contains(gitFile.path) ->
                computeUntrackedDiff(gitFile.path, repository)

            else -> computeTrackedDiff(gitFile.path, repository, git)
        }
    }.getOrNull()

    private fun computeUntrackedDiff(path: String, repository: Repository): DiffContent {
        val lines = repository.workTree.resolve(path).readText().lines()
        return DiffContent(
            oldPath = null,
            newPath = path,
            hunks = listOf(createDiffHunk(lines)),
        )
    }

    private fun computeTrackedDiff(path: String, repository: Repository, git: Git): DiffContent {
        val file = repository.workTree.resolve(path)
        val currentContent = file.readText()
        val currentLines = currentContent.lines()
        val previousContent = getPreviousContent(path, repository, git)
        val previousLines = previousContent.lines()
        val diffLines = compareLines(previousLines, currentLines)
        return createDiffContent(path, diffLines)
    }

    private fun getPreviousContent(path: String, repository: Repository, git: Git): String = runCatching {
        repository.resolve("HEAD")?.let {
            val reader = repository.newObjectReader()
            val treeId = repository.resolve("HEAD^{tree}")
            val oldTreeParser = CanonicalTreeParser().apply {
                reset(reader, treeId)
            }
            val newTreeParser = FileTreeIterator(repository)
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

    private fun createDiffContent(path: String, diffLines: List<Pair<DiffType, String>>): DiffContent {
        val hunks = mutableListOf<DiffHunk>()
        val currentHunkLines = mutableListOf<DiffLine>()
        var oldLineNum = 1
        var newLineNum = 1
        var hunkStart = true

        fun addCurrentHunk() {
            if (currentHunkLines.isNotEmpty()) {
                val oldCount = currentHunkLines.count { it.number.old != null }
                val newCount = currentHunkLines.count { it.number.new != null }
                val hunkHeader = "@@ -${oldLineNum - oldCount},$oldCount +${newLineNum - newCount},$newCount @@"
                hunks.add(
                    DiffHunk(
                        header = hunkHeader,
                        lines = currentHunkLines.toList(),
                    ),
                )
                currentHunkLines.clear()
                hunkStart = true
            }
        }

        diffLines.forEach { (type, line) ->
            when (type) {
                DiffType.ADDED -> {
                    if (hunkStart) addCurrentHunk()
                    hunkStart = false
                    currentHunkLines.add(
                        DiffLine(
                            number = DiffLine.LineNumbers(null, newLineNum++),
                            content = line,
                            type = DiffType.ADDED,
                        ),
                    )
                }
                DiffType.DELETED -> {
                    if (hunkStart) addCurrentHunk()
                    hunkStart = false
                    currentHunkLines.add(
                        DiffLine(
                            number = DiffLine.LineNumbers(oldLineNum++, null),
                            content = line,
                            type = DiffType.DELETED,
                        ),
                    )
                }
                DiffType.UNCHANGED -> {
                    currentHunkLines.add(
                        DiffLine(
                            number = DiffLine.LineNumbers(oldLineNum++, newLineNum++),
                            content = line,
                            type = DiffType.UNCHANGED,
                        ),
                    )
                }
            }
        }
        addCurrentHunk()
        return DiffContent(
            oldPath = path,
            newPath = path,
            hunks = hunks,
        )
    }

    private fun createDiffHunk(lines: List<String>): DiffHunk =
        DiffHunk(
            header = "@@ -0,0 +1,${lines.size} @@",
            lines = lines.mapIndexed { index, line ->
                DiffLine(
                    number = DiffLine.LineNumbers(null, index + 1),
                    content = line,
                    type = DiffType.ADDED,
                )
            },
        )

    private fun compareLines(oldLines: List<String>, newLines: List<String>): List<Pair<DiffType, String>> {
        val lcs = longestCommonSubsequence(oldLines, newLines)
        return buildDiffSequence(oldLines, newLines, lcs).toList()
    }

    private fun buildDiffSequence(
        oldLines: List<String>,
        newLines: List<String>,
        lcs: List<String>,
    ): Sequence<Pair<DiffType, String>> = sequence {
        var oldIndex = 0
        var newIndex = 0
        var lcsIndex = 0

        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            when {
                isMatchingLine(oldIndex, newIndex, lcsIndex, oldLines, newLines, lcs) -> {
                    yield(DiffType.UNCHANGED to oldLines[oldIndex])
                    oldIndex++; newIndex++; lcsIndex++
                }
                canDeleteOldLine(oldIndex, lcsIndex, oldLines, lcs)
                    && canAddNewLine(newIndex, lcsIndex, newLines, lcs)
                    && (oldLines[oldIndex].length - newLines[newIndex].length).absoluteValue < 10 -> {
                    yield(DiffType.ADDED to newLines[newIndex])
                    yield(DiffType.DELETED to oldLines[oldIndex])
                    oldIndex++
                    newIndex++
                }
                canAddNewLine(newIndex, lcsIndex, newLines, lcs) -> {
                    yield(DiffType.ADDED to newLines[newIndex])
                    newIndex++
                }
                canDeleteOldLine(oldIndex, lcsIndex, oldLines, lcs) -> {
                    yield(DiffType.DELETED to oldLines[oldIndex])
                    oldIndex++
                }
                else -> break
            }
        }
    }

    private fun isMatchingLine(
        oldIndex: Int,
        newIndex: Int,
        lcsIndex: Int,
        oldLines: List<String>,
        newLines: List<String>,
        lcs: List<String>,
    ): Boolean = lcsIndex < lcs.size
        && oldIndex < oldLines.size
        && newIndex < newLines.size
        && oldLines[oldIndex] == lcs[lcsIndex]
        && newLines[newIndex] == lcs[lcsIndex]

    private fun canAddNewLine(
        newIndex: Int,
        lcsIndex: Int,
        newLines: List<String>,
        lcs: List<String>,
    ): Boolean = newIndex < newLines.size
        && (lcsIndex >= lcs.size || newLines[newIndex] != lcs[lcsIndex])

    private fun canDeleteOldLine(
        oldIndex: Int,
        lcsIndex: Int,
        oldLines: List<String>,
        lcs: List<String>,
    ): Boolean = oldIndex < oldLines.size
        && (lcsIndex >= lcs.size || oldLines[oldIndex] != lcs[lcsIndex])
}
