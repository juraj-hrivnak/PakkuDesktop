/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import teksturepako.pakkuDesktop.app.git.log.GitLogTopology
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

    /** `git init -b [branch]` in [workDir]. The directory does not need to be a repository yet. */
    suspend fun initRepository(workDir: File, branch: String): Output =
        exec(workDir, "init", "-b", branch)

    /**
     * Branch name `git init` would use: global `init.defaultBranch`, or [DEFAULT_BRANCH_NAME].
     * [workDir] does not need to be a repository; a missing config or git failure falls back.
     */
    suspend fun suggestedDefaultBranch(workDir: File): String = try {
        val out = exec(workDir, "config", "--global", "--get", "init.defaultBranch")
        val configured = out.text.trim()
        if (out.ok && configured.isNotEmpty()) configured else DEFAULT_BRANCH_NAME
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        DEFAULT_BRANCH_NAME
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
        val refs = async {
            run(
                root, "for-each-ref", "--format=%(objectname)%00%(*objectname)%00%(refname)%00%(creatordate:iso8601)",
                "refs/heads", "refs/remotes", "refs/tags", "refs/stash",
            )
        }
        val remotes = async { run(root, "remote") }
        val headMessage = async { run(root, "log", "-1", "--format=%B") }

        val st = status.await()
        val refLines = refs.await()?.lineSequence()?.mapNotNull(::parseRefLine)?.toList().orEmpty()
        val refNames = refLines.map { it.second }
        val outgoing = async { readOutgoingCommits(root, st) }
        val stashes = async { if ("refs/stash" in refNames) readStashes(root) else emptyList() }
        val remotesList = remotes.await()?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toList()
            .orEmpty()
        val defaultRemote = remotesList.firstOrNull { it == "origin" } ?: remotesList.firstOrNull()
        val remoteTagNames = async { readRemoteTags(root, defaultRemote) }

        GitState(
            gitFiles = st.files.toPersistentList(),
            // An unborn branch (right after `git init`) has no ref yet, so for-each-ref omits it.
            branches = branchesFrom(refNames, st.head).let { parsed ->
                val head = st.head
                if (st.unborn && head != null && parsed.none { !it.isRemote && it.name == head }) {
                    parsed + GitBranch(name = head, isRemote = false, isCurrent = true)
                } else {
                    parsed
                }
            },
            outgoingCommits = outgoing.await(),
            upstream = st.upstream,
            incomingCount = st.behind,
            remotes = remotesList.toPersistentList(),
            headMessage = headMessage.await()?.trimEnd().orEmpty(),
            stashes = stashes.await().toPersistentList(),
            refs = refLines.mapNotNull { (hash, name, dateStr) -> gitRefOf(name, hash, dateStr) }.toPersistentList(),
            remoteTagNames = remoteTagNames.await(),
            headHash = st.headHash,
            isRepository = true,
        )
    }

    /** Fetches tag names from the remote via `git ls-remote --tags`. Falls back to empty set on any error. */
    private suspend fun readRemoteTags(root: File, remote: String?): Set<String> {
        if (remote == null) return emptySet()
        val output = run(root, "ls-remote", "--tags", remote) ?: return emptySet()
        return output.lineSequence()
            .mapNotNull { line ->
                // Format: HASH\trefs/tags/NAME  or  HASH\trefs/tags/NAME^{peel}
                val ref = line.substringAfter("\t")
                ref.takeIf { it.startsWith("refs/tags/") && !it.startsWith("refs/tags/^") }
                    ?.removePrefix("refs/tags/")
            }
            .toSet()
    }

    /** `objectname NUL *objectname NUL refname NUL creatordate`: the peeled hash of an annotated tag wins. */
    private fun parseRefLine(line: String): Triple<String, String, String?>? {
        val parts = line.trim().split('\u0000')
        if (parts.size < 3 || parts[2].isEmpty()) return null
        val dateStr = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3] else null
        return Triple(parts[1].ifEmpty { parts[0] }, parts[2], dateStr)
    }

    private fun gitRefOf(refname: String, hash: String, dateStr: String?): GitRef? = when {
        refname.startsWith("refs/heads/") -> GitRef(refname.removePrefix("refs/heads/"), GitRefType.LocalBranch, hash)
        refname.startsWith("refs/remotes/") && !refname.endsWith("/HEAD") ->
            GitRef(refname.removePrefix("refs/remotes/"), GitRefType.RemoteBranch, hash)
        refname.startsWith("refs/tags/") -> {
            val name = refname.removePrefix("refs/tags/")
            val createdAt = dateStr?.let { parseGitDate(it) }
            GitRef(name, GitRefType.Tag, hash, createdAt)
        }
        else -> null
    }

    /**
     * Parse git's ISO 8601 date format (e.g., "2026-10-17 00:01:48 +0200") into an [Instant].
     * Git uses space between date/time and timezone without colon; [Instant.parse] needs "T" and colon.
     */
    private fun parseGitDate(s: String): Instant? = runCatching {
        // "2026-10-17 00:01:48 +0200" -> "2026-10-17T00:01:48+02:00"
        val fixed = s.substring(0, 10) + "T" + s.substring(11).replace(" ", "")
        // Insert colon in timezone offset: "+0200" -> "+02:00"
        val lastPlus = fixed.lastIndexOf('+')
        if (lastPlus > 0 && lastPlus + 5 <= fixed.length) {
            val before = fixed.substring(0, lastPlus + 3)
            val after = fixed.substring(lastPlus + 3)
            Instant.parse(before + ":" + after)
        } else Instant.parse(fixed)
    }.getOrNull()

    private class StatusSnapshot(
        val files: List<GitFile>,
        /** null when detached */
        val head: String?,
        /** null before the first commit */
        val headHash: String? = null,
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
        var headHash: String? = null
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
                        rec.startsWith("# branch.oid ")      -> {
                            unborn = rec.endsWith("(initial)")
                            if (!unborn) headHash = rec.removePrefix("# branch.oid ")
                        }
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
            headHash = headHash,
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

    // -- Log --

    /** Revisions the Log shows: every branch and tag, plus a detached HEAD. Stashes are left out. */
    private fun logRevisions(headHash: String?): List<String> =
        listOfNotNull("--branches", "--remotes", "--tags", headHash?.let { "HEAD" })

    /**
     * Topology of the whole history (IntelliJ's permanent graph): hash, parents, author time and
     * author per commit, children before parents. Streams the output; subjects load separately.
     */
    suspend fun readLogTopology(workDir: File, headHash: String?): GitLogTopology
    {
        val root = workDir.canonicalFile
        val builder = GitLogTopology.Builder()
        val args = listOf("log") + logRevisions(headHash) +
            listOf("--date-order", "--no-show-signature", "--format=%H%x00%P%x00%at%x00%aN%x00%aE")
        val code = command(root, args, readOnly = true)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .forEachRecord('\n') { builder.add(it) }
        return if (code == 0) builder.build() else GitLogTopology.EMPTY
    }

    /** Subjects of [hashes] in one `git log --no-walk` call. Unknown hashes are skipped. */
    suspend fun readSubjects(workDir: File, hashes: Collection<String>): Map<String, String>
    {
        if (hashes.isEmpty()) return emptyMap()
        val output = run(workDir, "log", "--no-walk=unsorted", "--format=%H%x00%s", *hashes.toTypedArray())
            ?: return emptyMap()
        return output.lineSequence()
            .filter { '\u0000' in it }
            .associate { it.substringBefore('\u0000') to it.substringAfter('\u0000') }
    }

    /** Hashes of commits whose message matches [text], like IntelliJ's text filter. */
    suspend fun readMessageMatches(
        workDir: File,
        headHash: String?,
        text: String,
        matchCase: Boolean,
        regex: Boolean,
    ): Set<String>
    {
        val args = buildList {
            add("log")
            addAll(logRevisions(headHash))
            add("--format=%H")
            if (!matchCase) add("--regexp-ignore-case")
            add(if (regex) "--extended-regexp" else "--fixed-strings")
            add("--grep=$text")
        }
        val output = run(workDir, *args.toTypedArray()) ?: return emptySet()
        return output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
    }

    /** Message, people and changed files of [hash]; changes are against the first parent. */
    suspend fun readCommitDetails(workDir: File, hash: String): GitCommitDetails?
    {
        val output = run(
            workDir, "show", "-s", "--no-show-signature",
            "--format=%H%x00%P%x00%an%x00%ae%x00%at%x00%cn%x00%ce%x00%ct%x00%B", hash,
        ) ?: return null
        val parts = output.split('\u0000', limit = 9)
        if (parts.size != 9) return null
        val parents = parts[1].split(' ').filter { it.isNotEmpty() }
        val changes = if (parents.isEmpty())
        {
            run(workDir, "diff-tree", "-r", "--root", "--no-commit-id", "-M", "--name-status", "-z", hash)
        }
        else
        {
            run(workDir, "diff", "-M", "--name-status", "-z", parents[0], hash)
        }
        return GitCommitDetails(
            hash = parts[0].trim(),
            parents = parents,
            authorName = parts[2],
            authorEmail = parts[3],
            authorTime = Instant.fromEpochSeconds(parts[4].toLongOrNull() ?: 0L),
            committerName = parts[5],
            committerEmail = parts[6],
            commitTime = Instant.fromEpochSeconds(parts[7].toLongOrNull() ?: 0L),
            message = parts[8].trimEnd(),
            changes = parseNameStatus(changes.orEmpty()),
        )
    }

    /** `--name-status -z`: `X NUL path NUL`, or `Rnnn NUL old NUL new NUL` for renames and copies. */
    internal fun parseNameStatus(output: String): List<GitCommitChange>
    {
        val fields = output.split('\u0000')
        val changes = mutableListOf<GitCommitChange>()
        var i = 0
        while (i < fields.size)
        {
            val code = fields[i].trim()
            if (code.isEmpty())
            {
                i++
                continue
            }
            if (code[0] == 'R' || code[0] == 'C')
            {
                val old = fields.getOrNull(i + 1) ?: break
                val new = fields.getOrNull(i + 2) ?: break
                changes += if (code[0] == 'R') GitCommitChange(new, GitChange.Modified(new), oldPath = old)
                else GitCommitChange(new, GitChange.Added(new), oldPath = old)
                i += 3
            }
            else
            {
                val path = fields.getOrNull(i + 1) ?: break
                val status = when (code[0])
                {
                    'A'  -> GitChange.Added(path)
                    'D'  -> GitChange.Deleted(path)
                    else -> GitChange.Modified(path)
                }
                changes += GitCommitChange(path, status)
                i += 2
            }
        }
        return changes.sortedBy { it.path }
    }

    /** Branches (local and remote) that contain [hash], IntelliJ's "In N branches". */
    suspend fun readContainingBranches(workDir: File, hash: String): List<String>
    {
        val output = run(workDir, "branch", "-a", "--contains", hash, "--format=%(refname)") ?: return emptyList()
        return output.lineSequence().map { it.trim() }.mapNotNull { ref ->
            when
            {
                ref.startsWith("refs/heads/")                                 -> ref.removePrefix("refs/heads/")
                ref.startsWith("refs/remotes/") && !ref.endsWith("/HEAD") -> ref.removePrefix("refs/remotes/")
                else                                                          -> null
            }
        }.toList()
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

    /** One file of commit [hash] against [parent] (its first parent; null for a root commit). */
    suspend fun computeCommitDiff(workDir: File, hash: String, parent: String?, change: GitCommitChange): DiffContent
    {
        val root = workDir.canonicalFile
        val path = change.path
        val oldPath = change.oldPath ?: path
        val deleted = change.status is GitChange.Deleted
        val created = change.status is GitChange.Added && change.oldPath == null
        val newBytes = if (deleted) null else run(root, "cat-file", "-s", "$hash:$path")?.trim()?.toLongOrNull()
        val oldBytes = if (parent == null || created) null
        else run(root, "cat-file", "-s", "$parent:$oldPath")?.trim()?.toLongOrNull()
        val largest = maxOf(newBytes ?: 0L, oldBytes ?: 0L)
        if (largest > MAX_FILE_BYTES) return tooLargeDiff(path, largest)

        val paths = listOf(oldPath, path).distinct()
        val args = if (parent != null) listOf("diff", "-M", parent, hash, "--") + paths
        else listOf("diff-tree", "-p", "-M", "--root", "--no-commit-id", hash, "--") + paths
        val captured = try
        {
            paths.forEach { requireInside(root, it) }
            command(root, args, readOnly = true)
                .redirectError(ProcessBuilder.Redirect.DISCARD).runCapturingCapped(MAX_FILE_BYTES.toInt())
        }
        catch (e: CancellationException)
        {
            throw e
        }
        catch (_: Exception)
        {
            return placeholderDiffContent(path, "Diff not available.")
        }
        if (captured.truncated) return tooLargeDiff(path, captured.text.length.toLong())
        val parsed = UnifiedDiffParser.parse(captured.text, path)
        if (parsed.hunks.isEmpty())
        {
            val binary = "Binary files" in captured.text
            return placeholderDiffContent(path, if (binary) "Binary file not shown." else "No content changes.")
        }
        val text = if (deleted) null else run(root, "show", "$hash:$path")
        val context = if (text == null || text.length > MAX_FILE_BYTES || text.indexOf('\u0000') >= 0) emptyList()
        else text.lines()
        return UnifiedDiffParser.withEnclosingContext(parsed, context)
    }

    private fun tooLargeDiff(path: String, bytes: Long) =
        placeholderDiffContent(path, "File too large to display (${bytes / 1024} KB).")

    private fun untrackedAsNewFile(workDir: File, path: String): DiffContent
    {
        val root = workDir.canonicalFile
        val file =
            File(root, path).canonicalFile.takeIf { it.path.startsWith(root.path + File.separator) || it == root }
                ?: return placeholderDiffContent(path, "INVALID PATH")
        if (file.isDirectory) return placeholderDiffContent(path, "Directory, select individual files.")
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

