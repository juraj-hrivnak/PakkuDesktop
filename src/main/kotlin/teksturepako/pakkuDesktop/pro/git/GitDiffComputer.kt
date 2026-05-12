/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.errors.MissingObjectException
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

    private const val MAX_FILE_BYTES = 512_000L
    private const val MAX_RENDERED_LINES = 5_000
    private const val BINARY_PROBE_BYTES = 8_000

    fun openRepository(path: Path): Result<Pair<Git, Repository>> = runCatching {
        val repository = FileRepositoryBuilder()
            .setGitDir(path.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build()
        Git(repository) to repository
    }

    /**
     * Working tree vs HEAD for [gitFile]. Uses JGit's formatter (same algorithm as native git diff).
     * Throws on JGit / IO errors so the caller can surface them (e.g. via a toast).
     */
    fun computeDiff(git: Git, repository: Repository, gitFile: GitFile): DiffContent {
        val path = gitFile.path
        if (gitFile.status is GitChange.Untracked) {
            return untrackedAsNewFile(repository, path)
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
            return untrackedAsNewFile(repository, path)
        }

        val raw = ByteArrayOutputStream()
        try {
            DiffFormatter(raw).use { fmt ->
                fmt.setRepository(repository)
                fmt.setContext(3)
                for (entry in entries) {
                    fmt.format(entry)
                }
            }
        } catch (e: MissingObjectException) {
            // JGit cannot read the blob from its pack-file reader even though native git can
            // (common with certain pack-index versions, multi-pack-index, or alternates).
            // Fall back to invoking the native git binary which has no such limitation.
            println("[GitDiff] MissingObjectException for $path (${e.objectId.name}), falling back to native git diff")
            return nativeGitDiff(repository, path)
        }

        if (raw.size() > MAX_FILE_BYTES) {
            return placeholderDiffContent(path, "Diff too large to display (${raw.size() / 1024} KB).")
        }

        val text = raw.toString(StandardCharsets.UTF_8)
        if (text.isBlank()) {
            return untrackedAsNewFile(repository, path)
        }
        return UnifiedDiffParser.parse(text, path)
    }

    /**
     * Runs `git diff HEAD -- <path>` as a subprocess in the repository's working directory.
     * Used as a fallback when JGit cannot read a blob from the pack-file store.
     * If the subprocess fails or produces no output, falls back to [untrackedAsNewFile].
     */
    private fun nativeGitDiff(repository: Repository, path: String): DiffContent {
        return try {
            val process = ProcessBuilder("git", "diff", "HEAD", "--", path)
                .directory(repository.workTree)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            println("[GitDiff] Native git diff exited $exitCode, output length=${output.length}")
            if (exitCode == 0 && output.isNotBlank()) {
                UnifiedDiffParser.parse(output, path)
            } else {
                untrackedAsNewFile(repository, path)
            }
        } catch (e: Exception) {
            println("[GitDiff] Native git diff failed: ${e::class.simpleName}: ${e.message}")
            untrackedAsNewFile(repository, path)
        }
    }

    private fun untrackedAsNewFile(repository: Repository, path: String): DiffContent {
        val file = repository.workTree.resolve(path)

        if (isBinaryFile(file)) {
            return placeholderDiffContent(path, "Binary file not shown.")
        }

        if (file.length() > MAX_FILE_BYTES) {
            return placeholderDiffContent(path, "File too large to display (${file.length() / 1024} KB).")
        }

        val lines = file.readText().lines().take(MAX_RENDERED_LINES)
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

    private fun placeholderDiffContent(path: String, message: String) = DiffContent(
        oldPath = null,
        newPath = path,
        hunks = listOf(
            DiffHunk(
                header = "@@",
                lines = listOf(
                    DiffLine(
                        number = DiffLine.LineNumbers(old = null, new = null),
                        content = message,
                        type = DiffType.UNCHANGED,
                    ),
                ),
            ),
        ),
    )

    private fun isBinaryFile(file: java.io.File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val probe = file.inputStream().use { it.readNBytes(BINARY_PROBE_BYTES) }
        return probe.any { it == 0.toByte() }
    }
}
