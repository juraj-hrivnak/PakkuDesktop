/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git.wrapper

import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffHunk
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffLine
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitCommit
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import java.io.File
import java.nio.file.Path

/**
 * Unified API for all native git operations. All git process execution goes through [run].
 * Call methods from an IO dispatcher.
 */
object NativeGit {

    private const val MAX_FILE_BYTES = 512_000L
    private const val MAX_RENDERED_LINES = 5_000
    private const val BINARY_PROBE_BYTES = 8_000

    // -------------------------------------------------------------------------
    // Repository
    // -------------------------------------------------------------------------

    /**
     * Verifies [path] contains a `.git` directory and returns the working-tree [File].
     */
    fun openRepository(path: Path): Result<File> = runCatching {
        val workDir = path.toFile()
        require(workDir.resolve(".git").exists()) { "Not a git repository: $path" }
        workDir
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Builds [GitState] purely from native git commands.
     */
    fun buildState(workDir: File, preserve: GitState): GitState = preserve.copy(
        branches = readBranches(workDir),
        outgoingCommits = readOutgoingCommits(workDir),
        gitFiles = parseGitStatus(workDir),
    )

    private fun parseGitStatus(workDir: File): List<GitFile> {
        // -uall expands untracked directories into individual files so the tree
        // builder never receives a bare "dir/" path that would be mis-classified
        // as a file and crash on diff.
        val output = run(workDir, "status", "--porcelain", "-uall") ?: return emptyList()
        val pathToChange = linkedMapOf<String, GitChange>()

        for (raw in output.lineSequence()) {
            if (raw.length < 4) continue
            val xy = raw.substring(0, 2)
            // Renames are shown as "old -> new"; take the destination path.
            var path = raw.substring(3)
            if (" -> " in path) path = path.substringAfterLast(" -> ")
            path = path.trim().removeSurrounding("\"")

            val change: GitChange = when {
                xy == "??"                    -> GitChange.Untracked(path)
                xy[0] == 'A'                  -> GitChange.Added(path)
                xy[0] == 'D' || xy[1] == 'D' -> GitChange.Deleted(path)
                else                          -> GitChange.Modified(path)
            }
            pathToChange[path] = change
        }

        return pathToChange.entries.sortedBy { it.key }.mapNotNull { (path, change) ->
            val root = workDir.canonicalFile
            val file = File(root, path).canonicalFile
                .takeIf { it.path.startsWith(root.path + File.separator) || it == root }
                ?: return@mapNotNull null
            GitFile.fromFileInfo(
                path = path.replace(File.separatorChar, '/'),
                status = change,
                modifiedEpochMillis = file.lastModified(),
                size = file.length(),
            )
        }
    }

    private fun readBranches(workDir: File): Set<GitBranch> {
        val currentBranch = run(workDir, "branch", "--show-current")?.trim().orEmpty()
        val output = run(workDir, "for-each-ref", "--format=%(refname)", "refs/heads", "refs/remotes")
            ?: return emptySet()

        return output.lineSequence()
            .filter { it.isNotBlank() }
            .map { refname ->
                val isRemote = refname.startsWith("refs/remotes/")
                val name = when {
                    refname.startsWith("refs/heads/")   -> refname.removePrefix("refs/heads/")
                    refname.startsWith("refs/remotes/") -> refname.removePrefix("refs/remotes/")
                    else                                -> refname
                }
                GitBranch(name = name, isRemote = isRemote, isCurrent = !isRemote && name == currentBranch)
            }.toSet()
    }

    private fun readOutgoingCommits(workDir: File): Set<GitCommit> {
        // `@{u}` fails gracefully when no upstream is configured — run returns null/empty.
        val output = run(workDir, "log", "@{u}..HEAD", "--format=%h %s") ?: return emptySet()
        return output.lineSequence()
            .filter { it.isNotBlank() }
            .take(100)
            .mapTo(LinkedHashSet()) { line ->
                val space = line.indexOf(' ')
                if (space < 0) GitCommit(line.trim(), "")
                else GitCommit(line.substring(0, space), line.substring(space + 1).trim())
            }
    }

    // -------------------------------------------------------------------------
    // Diff
    // -------------------------------------------------------------------------

    /**
     * Computes the diff for [gitFile] against HEAD using `git diff`.
     * Untracked files are rendered as fully-added new files.
     */
    fun computeDiff(workDir: File, gitFile: GitFile): DiffContent {
        val path = gitFile.path
        if (gitFile.status is GitChange.Untracked) return untrackedAsNewFile(workDir, path)
        val output = run(workDir, "diff", "HEAD", files = arrayOf(path))
        return if (!output.isNullOrBlank()) {
            UnifiedDiffParser.parse(output, path)
        } else {
            untrackedAsNewFile(workDir, path)
        }
    }

    private fun untrackedAsNewFile(workDir: File, path: String): DiffContent {
        val root = workDir.canonicalFile
        val file = File(root, path).canonicalFile
            .takeIf { it.path.startsWith(root.path + File.separator) || it == root }
            ?: return placeholderDiffContent(path, "INVALID PATH")
        if (file.isDirectory) return placeholderDiffContent(path, "Directory — select individual files.")
        if (isBinaryFile(file)) return placeholderDiffContent(path, "Binary file not shown.")
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

    private fun isBinaryFile(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val probe = file.inputStream().use { it.readNBytes(BINARY_PROBE_BYTES) }
        return probe.any { it == 0.toByte() }
    }

    fun run(workDir: File, vararg args: String, files: Array<out String> = emptyArray()): String? = try {
        val root = workDir.canonicalFile
        val f = files.map { rel ->
            val resolved = File(root, rel).canonicalFile
            check(resolved.path.startsWith(root.path + File.separator) || resolved == root) {
                "INVALID PATH $rel"
            }
            rel
        }
        val allArgs = if (f.isEmpty()) args.toList() else args.toList() + "--" + f
        val proc = ProcessBuilder("git", *allArgs.toTypedArray())
            .directory(root)
            .redirectErrorStream(false)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        output
    } catch (_: Exception) {
        null
    }
}

