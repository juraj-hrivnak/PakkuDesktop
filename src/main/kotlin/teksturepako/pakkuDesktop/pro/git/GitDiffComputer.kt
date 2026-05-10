/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffHunk
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffLine
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object GitDiffComputer {

    fun openRepository(path: Path): Result<Pair<Git, Repository>> = runCatching {
        val repository = FileRepositoryBuilder()
            .setGitDir(path.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build()
        Git(repository) to repository
    }

    /**
     * Working tree vs HEAD for [gitFile.path]. Uses JGit's formatter (same algorithm as native git diff).
     */
    fun computeDiff(git: Git, repository: Repository, gitFile: GitFile): DiffContent? = runCatching {
        val path = gitFile.path
        if (gitFile.status is GitChange.Untracked) {
            return@runCatching untrackedAsNewFile(repository, path)
        }

        val entries = repository.newObjectReader().use { reader ->
            val headTree = repository.resolve("HEAD^{tree}")
            val oldTree = if (headTree != null) {
                CanonicalTreeParser().apply { reset(reader, headTree) }
            } else {
                EmptyTreeIterator()
            }
            val newTree = FileTreeIterator(repository)
            git.diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .setPathFilter(PathFilter.create(path))
                .call()
        }

        if (entries.isEmpty()) {
            return@runCatching untrackedAsNewFile(repository, path)
        }

        val raw = ByteArrayOutputStream()
        DiffFormatter(raw).use { fmt ->
            fmt.setRepository(repository)
            fmt.setContext(3)
            for (entry in entries) {
                fmt.format(entry)
            }
        }

        val text = raw.toString(StandardCharsets.UTF_8)
        if (text.isBlank()) {
            return@runCatching untrackedAsNewFile(repository, path)
        }
        UnifiedDiffParser.parse(text, path)
    }.getOrNull()

    private fun untrackedAsNewFile(repository: Repository, path: String): DiffContent {
        val text = repository.workTree.resolve(path).readText()
        val lines = text.lines()
        return DiffContent(
            oldPath = null,
            newPath = path,
            hunks = listOf(
                DiffHunk(
                    header = "@@ -0,0 +1,${lines.size} @@",
                    lines = lines.mapIndexed { i, line ->
                        DiffLine(
                            number = DiffLine.LineNumbers(old = null, new = i + 1),
                            content = line,
                            type = DiffType.ADDED,
                        )
                    },
                ),
            ),
        )
    }
}
