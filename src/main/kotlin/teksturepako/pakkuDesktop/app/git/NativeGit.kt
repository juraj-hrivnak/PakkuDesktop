/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unified API for all native git operations. All git process execution goes through [run].
 * Call methods from an IO dispatcher.
 */
object NativeGit
{

    private const val MAX_FILE_BYTES = 512_000L
    private const val MAX_RENDERED_LINES = 5_000
    private const val BINARY_PROBE_BYTES = 8_000

    // -- Repository --

    /**
     * Verifies [path] contains a `.git` directory and returns the working-tree [File].
     */
    fun openRepository(path: Path): Result<File> = runCatching {
        val workDir = path.toFile()
        require(workDir.resolve(".git").exists()) { "Not a git repository: $path" }
        workDir
    }

    // -- Process --

    /**
     * Every git invocation goes through here. [readOnly] commands run without optional locks so a
     * background refresh never rewrites `.git/index` or races a user command for `index.lock`.
     * Prompts are disabled: a GUI app has no terminal, so git would otherwise wait forever.
     */
    internal fun command(root: File, args: List<String>, readOnly: Boolean): ProcessBuilder =
        ProcessBuilder(listOf("git", "-c", "core.quotepath=false") + args).directory(root).apply {
            val env = environment()
            env["GIT_TERMINAL_PROMPT"] = "0"
            env["GIT_EDITOR"] = "true"
            env["GIT_MERGE_AUTOEDIT"] = "no"
            if (readOnly) env["GIT_OPTIONAL_LOCKS"] = "0"
        }

    // -- State --

    /**
     * Reads the repository part of [GitState]; UI-only fields stay at their defaults.
     * Independent reads run in parallel, so the wall time is about two git spawns.
     */
    suspend fun readState(workDir: File): GitState = coroutineScope {
        val root = workDir.canonicalFile
        val status = async { readStatus(root) }
        val refs = async { run(root, "for-each-ref", "--format=%(refname)", "refs/heads", "refs/remotes", "refs/stash") }
        val remotes = async { run(root, "remote") }
        val headMessage = async { run(root, "log", "-1", "--format=%B") }

        val st = status.await()
        val refNames = refs.await()?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toList().orEmpty()
        val outgoing = async { readOutgoingCommits(root, st) }
        val stashes = async { if ("refs/stash" in refNames) readStashes(root) else emptyList() }

        GitState(
            gitFiles = st.files.toPersistentList(),
            branches = branchesFrom(refNames, st.head),
            outgoingCommits = outgoing.await(),
            upstream = st.upstream,
            incomingCount = st.behind,
            remotes = remotes.await()?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toList()
                .orEmpty().toPersistentList(),
            headMessage = headMessage.await()?.trimEnd().orEmpty(),
            stashes = stashes.await().toPersistentList(),
        )
    }

    private class StatusSnapshot(
        val files: List<GitFile>,
        /** null when detached */
        val head: String?,
        val unborn: Boolean,
        /** null without an upstream, or when the upstream ref is gone */
        val upstream: String?,
        val ahead: Int,
        val behind: Int,
    )

    /** One `git status --porcelain=v2 --branch -z` call: changes plus branch, upstream and ahead/behind. */
    private suspend fun readStatus(root: File): StatusSnapshot
    {
        val changes = HashMap<String, GitChange>()
        var head: String? = null
        var unborn = false
        var upstream: String? = null
        var ab: Pair<Int, Int>? = null
        var skipOrigPath = false
        try
        {
            command(root, listOf("status", "--porcelain=v2", "--branch", "-z", "--untracked-files=all"), readOnly = true)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .forEachRecord('\u0000') { rec ->
                    if (skipOrigPath)
                    {
                        skipOrigPath = false
                        return@forEachRecord
                    }
                    when
                    {
                        rec.startsWith("# branch.oid ")      -> unborn = rec.endsWith("(initial)")
                        rec.startsWith("# branch.head ")     ->
                            head = rec.removePrefix("# branch.head ").takeIf { it != "(detached)" }
                        rec.startsWith("# branch.upstream ") -> upstream = rec.removePrefix("# branch.upstream ")
                        rec.startsWith("# branch.ab ")       -> ab = parseAheadBehind(rec.removePrefix("# branch.ab "))
                        rec.startsWith("1 ")                 -> rec.split(' ', limit = 9).let { f ->
                            if (f.size == 9) changes[f[8]] = trackedChange(f[1], f[8])
                        }
                        rec.startsWith("2 ")                 -> {
                            // Rename/copy: the original path follows as its own NUL record.
                            skipOrigPath = true
                            rec.split(' ', limit = 10).let { f -> if (f.size == 10) changes[f[9]] = GitChange.Modified(f[9]) }
                        }
                        rec.startsWith("u ")                 -> rec.split(' ', limit = 11).let { f ->
                            if (f.size == 11) changes[f[10]] = GitChange.Modified(f[10])
                        }
                        rec.startsWith("? ")                 -> rec.substring(2).let { changes[it] = GitChange.Untracked(it) }
                    }
                }
        }
        catch (e: CancellationException)
        {
            throw e
        }
        catch (_: Exception)
        {
            return StatusSnapshot(emptyList(), null, unborn = false, upstream = null, ahead = 0, behind = 0)
        }

        val files = changes.entries.sortedBy { it.key }.mapNotNull { (path, change) ->
            val file = root.childOrNull(path) ?: return@mapNotNull null
            val attrs = try
            {
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            }
            catch (_: Exception)
            {
                null // deleted in the working tree
            }
            GitFile.fromFileInfo(
                path = path,
                status = change,
                modifiedEpochMillis = attrs?.lastModifiedTime()?.toMillis() ?: 0L,
                size = attrs?.size() ?: 0L,
            )
        }
        // Without branch.ab the upstream ref is gone; `@{u}` would fail, so treat it as untracked.
        val counts = ab
        return StatusSnapshot(
            files = files,
            head = head,
            unborn = unborn,
            upstream = if (counts != null) upstream else null,
            ahead = counts?.first ?: 0,
            behind = counts?.second ?: 0,
        )
    }

    private fun parseAheadBehind(value: String): Pair<Int, Int>?
    {
        val parts = value.split(' ')
        val ahead = parts.getOrNull(0)?.removePrefix("+")?.toIntOrNull() ?: return null
        val behind = parts.getOrNull(1)?.removePrefix("-")?.toIntOrNull() ?: return null
        return ahead to behind
    }

    /** Porcelain v2 XY: index then worktree, `.` = unchanged. */
    private fun trackedChange(xy: String, path: String): GitChange = when
    {
        xy.getOrNull(0) == 'A'                               -> GitChange.Added(path)
        xy.getOrNull(0) == 'D' || xy.getOrNull(1) == 'D' -> GitChange.Deleted(path)
        else                                                 -> GitChange.Modified(path)
    }

    /** Repo-relative [rel] that stays inside this directory. Does not touch the filesystem. */
    private fun File.childOrNull(rel: String): File?
    {
        if (rel.isBlank() || rel.startsWith('/') || rel.startsWith('\\') || File(rel).isAbsolute) return null
        val parts = rel.split('/', '\\')
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) return null
        val file = File(this, rel)
        val rootPath = path
        val childPath = file.path
        if (childPath != rootPath && !childPath.startsWith(rootPath + File.separator)) return null
        return file
    }

    private fun branchesFrom(refNames: List<String>, currentBranch: String?): Set<GitBranch> =
        refNames.mapNotNullTo(LinkedHashSet()) { refname ->
            when
            {
                refname.startsWith("refs/heads/")   -> refname.removePrefix("refs/heads/").let { name ->
                    GitBranch(name = name, isRemote = false, isCurrent = name == currentBranch)
                }
                // `origin/HEAD` is a symref to the default branch, not a branch of its own.
                refname.startsWith("refs/remotes/") && !refname.endsWith("/HEAD") ->
                    GitBranch(name = refname.removePrefix("refs/remotes/"), isRemote = true, isCurrent = false)
                else                                -> null
            }
        }

    private suspend fun readOutgoingCommits(root: File, status: StatusSnapshot): Set<GitCommit>
    {
        if (status.unborn) return emptySet()
        // Without an upstream, everything not on any remote is outgoing (first push of a branch).
        val output = if (status.upstream != null)
        {
            if (status.ahead == 0) return emptySet()
            run(root, "log", "-n", "100", "@{u}..HEAD", "--format=%h %s")
        }
        else
        {
            run(root, "log", "-n", "100", "HEAD", "--not", "--remotes", "--format=%h %s")
        } ?: return emptySet()
        return output.lineSequence().filter { it.isNotBlank() }.take(100).mapTo(LinkedHashSet()) { line ->
                val space = line.indexOf(' ')
                if (space < 0) GitCommit(line.trim(), "")
                else GitCommit(line.substring(0, space), line.substring(space + 1).trim())
            }
    }

    private suspend fun readStashes(workDir: File): List<GitStash>
    {
        val output = run(workDir, "stash", "list", "--format=%gd%x00%s") ?: return emptyList()
        return output.lineSequence().filter { '\u0000' in it }
            .map { line -> GitStash(ref = line.substringBefore('\u0000'), message = line.substringAfter('\u0000')) }
            .toList()
    }

    // -- Operations --

    /** Exit code and merged stdout/stderr of one git command. */
    data class Output(val exitCode: Int, val text: String)
    {
        val ok: Boolean get() = exitCode == 0

        /** Last meaningful line, for a toast. */
        fun failure(fallback: String): String =
            text.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() && !it.startsWith("hint:") } ?: fallback
    }

    /** Runs git with stderr merged into the captured output. [env] is added to the environment. */
    suspend fun exec(
        workDir: File,
        vararg args: String,
        files: List<String> = emptyList(),
        env: Map<String, String> = emptyMap(),
    ): Output
    {
        val root = workDir.canonicalFile
        files.forEach { rel -> requireInside(root, rel) }
        val allArgs = if (files.isEmpty()) args.toList() else args.toList() + "--" + files
        val (code, text) = command(root, allArgs, readOnly = false).redirectErrorStream(true)
            .apply { environment().putAll(env) }.runCapturing()
        return Output(code, text)
    }

    /**
     * Commits exactly [paths] (like IntelliJ's partial commit): a temporary index starts at HEAD,
     * takes the working-tree state of [paths], and is committed. Anything else the user staged
     * stays staged; the real index entries of [paths] are then synced to the new HEAD.
     */
    suspend fun commitPaths(workDir: File, message: String, paths: List<String>, amend: Boolean): Output
    {
        val tempIndex = withContext(Dispatchers.IO) {
            File.createTempFile("pakku-commit", ".index")
        }.apply { delete() }

        try
        {
            val env = mapOf("GIT_INDEX_FILE" to tempIndex.absolutePath)
            val hasHead = exec(workDir, "rev-parse", "--verify", "-q", "HEAD").ok
            val read = if (hasHead) exec(workDir, "read-tree", "HEAD", env = env)
            else exec(workDir, "read-tree", "--empty", env = env)
            if (!read.ok) return read
            val add = exec(workDir, "add", "-A", files = paths, env = env)
            if (!add.ok) return add
            val commitArgs = buildList {
                add("commit")
                if (amend) add("--amend")
                add("-m")
                add(message)
            }
            val commit = exec(workDir, *commitArgs.toTypedArray(), env = env)
            if (!commit.ok) return commit
            exec(workDir, "reset", "-q", files = paths)
            return commit
        }
        finally
        {
            tempIndex.delete()
        }
    }

    /** Restores tracked [trackedPaths] to HEAD; unstages [addedPaths] and optionally deletes them. */
    suspend fun rollback(
        workDir: File,
        trackedPaths: List<String>,
        addedPaths: List<String>,
        deleteAdded: Boolean,
    ): Output
    {
        if (trackedPaths.isNotEmpty())
        {
            val restored = exec(workDir, "checkout", "HEAD", files = trackedPaths)
            if (!restored.ok) return restored
        }
        if (addedPaths.isNotEmpty())
        {
            val unstaged = exec(workDir, "rm", "--cached", "-q", "-r", "--force", files = addedPaths)
            if (!unstaged.ok) return unstaged
            if (deleteAdded)
            {
                val root = workDir.canonicalFile
                addedPaths.forEach { requireInside(root, it).delete() }
            }
        }
        return Output(0, "")
    }

    /** Appends anchored entries for [paths] to the root `.gitignore`, skipping ones already listed. */
    fun ignore(workDir: File, paths: List<String>)
    {
        val root = workDir.canonicalFile
        paths.forEach { requireInside(root, it) }
        val gitignore = File(root, ".gitignore")
        val existing = if (gitignore.exists()) gitignore.readText() else ""
        val present = existing.lineSequence().map { it.trim() }.toSet()
        val entries = paths.map { "/" + it.replace(File.separatorChar, '/') }.filter { it !in present }
        if (entries.isEmpty()) return
        val prefix = if (existing.isEmpty() || existing.endsWith("\n")) "" else "\n"
        gitignore.appendText(prefix + entries.joinToString("\n", postfix = "\n"))
    }

    private fun requireInside(root: File, rel: String): File
    {
        val resolved = File(root, rel).canonicalFile
        check(resolved.path.startsWith(root.path + File.separator) || resolved == root) { "INVALID PATH $rel" }
        return resolved
    }

    // -- Diff --

    /**
     * Computes the diff for [gitFile] against HEAD using `git diff`.
     * Untracked files are rendered as fully-added new files.
     */
    suspend fun computeDiff(workDir: File, gitFile: GitFile): DiffContent
    {
        val path = gitFile.path
        if (gitFile.status is GitChange.Untracked) return untrackedAsNewFile(workDir, path)
        if (gitFile.size > MAX_FILE_BYTES) return tooLargeDiff(path, gitFile.size)
        val blobBytes = run(workDir, "cat-file", "-s", "HEAD:$path")?.trim()?.toLongOrNull()
        if (blobBytes != null && blobBytes > MAX_FILE_BYTES) return tooLargeDiff(path, blobBytes)
        val root = workDir.canonicalFile
        val captured = try
        {
            requireInside(root, path)
            command(root, listOf("diff", "HEAD", "--", path), readOnly = true)
                .redirectError(ProcessBuilder.Redirect.DISCARD).runCapturingCapped(MAX_FILE_BYTES.toInt())
        }
        catch (e: CancellationException)
        {
            throw e
        }
        catch (_: Exception)
        {
            return untrackedAsNewFile(workDir, path)
        }
        return when
        {
            captured.truncated         -> tooLargeDiff(path, captured.text.length.toLong())
            captured.text.isNotBlank() -> UnifiedDiffParser.withEnclosingContext(
                UnifiedDiffParser.parse(captured.text, path),
                contextLines(root, path),
            )
            else                       -> untrackedAsNewFile(workDir, path)
        }
    }

    private fun tooLargeDiff(path: String, bytes: Long) =
        placeholderDiffContent(path, "File too large to display (${bytes / 1024} KB).")

    private fun untrackedAsNewFile(workDir: File, path: String): DiffContent
    {
        val root = workDir.canonicalFile
        val file =
            File(root, path).canonicalFile.takeIf { it.path.startsWith(root.path + File.separator) || it == root }
                ?: return placeholderDiffContent(path, "INVALID PATH")
        if (file.isDirectory) return placeholderDiffContent(path, "Directory — select individual files.")
        if (isBinaryFile(file)) return placeholderDiffContent(path, "Binary file not shown.")
        if (file.length() > MAX_FILE_BYTES)
        {
            return placeholderDiffContent(path, "File too large to display (${file.length() / 1024} KB).")
        }
        val lines = file.readText().lines().take(MAX_RENDERED_LINES)
        return UnifiedDiffParser.withEnclosingContext(
            DiffContent(
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
            ),
            lines,
        )
    }

    private suspend fun contextLines(root: File, path: String): List<String> {
        val onDisk = root.childOrNull(path)
        if (onDisk != null && onDisk.isFile && onDisk.length() <= MAX_FILE_BYTES && !isBinaryFile(onDisk)) {
            return onDisk.readLines()
        }
        val text = run(root, "show", "HEAD:$path") ?: return emptyList()
        if (text.length > MAX_FILE_BYTES || text.indexOf('\u0000') >= 0) return emptyList()
        return text.lines()
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

    private fun isBinaryFile(file: File): Boolean
    {
        if (!file.exists() || file.length() == 0L) return false
        val probe = file.inputStream().use { it.readNBytes(BINARY_PROBE_BYTES) }
        return probe.any { it == 0.toByte() }
    }

    /**
     * Clones [url] into [destDir] (created as the repository root).
     * Runs `git clone <url> <destDir.name>` in the parent directory.
     */
    suspend fun clone(url: String, destDir: File): Result<File> = runCatching {
        require(url.isNotBlank()) { "Clone URL is blank" }
        val parent = destDir.parentFile ?: error("Invalid destination: $destDir")
        parent.mkdirs()
        require(!destDir.exists() || destDir.list().isNullOrEmpty()) {
            "Destination already exists and is not empty: $destDir"
        }
        val (code, output) = command(parent, listOf("clone", url, destDir.name), readOnly = false)
            .redirectErrorStream(true).runCapturing()
        check(code == 0) { output.ifBlank { "git clone failed with exit code $code" } }
        destDir
    }.onFailure { if (it is CancellationException) throw it }

    /** Read-only command; stdout on exit code 0, else null. */
    suspend fun run(workDir: File, vararg args: String, files: Array<out String> = emptyArray()): String? = try
    {
        val root = workDir.canonicalFile
        files.forEach { rel -> requireInside(root, rel) }
        val allArgs = if (files.isEmpty()) args.toList() else args.toList() + "--" + files
        val (code, text) = command(root, allArgs, readOnly = true).redirectError(ProcessBuilder.Redirect.DISCARD)
            .runCapturing()
        text.takeIf { code == 0 }
    }
    catch (e: CancellationException)
    {
        throw e
    }
    catch (_: Exception)
    {
        null
    }
}

