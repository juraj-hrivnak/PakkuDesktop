/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.actions.actionInfoToast
import teksturepako.pakkuDesktop.app.git.*
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.conflatedPublisher
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// -- Repository worker --

/**
 * One background worker per open repository, in the spirit of IntelliJ's git integration:
 * refreshes are coalesced (a request during a refresh collapses into one follow-up), and ops
 * and refreshes never overlap. Work runs in the driver's long-lived scope, so a recomposition
 * or a model reload never cancels an op halfway and leaves the UI busy.
 */
private class GitRepositoryWorker(
    private val scope: CoroutineScope,
    private val publish: (AppMsg) -> Unit,
    private val latestModel: () -> AppModel,
) {
    private val lock = Mutex()
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var root: File? = null

    /** Serves [requestRefresh] for as long as the caller is active. */
    suspend fun processRefreshes() = withContext(Dispatchers.IO) {
        for (request in refreshRequests) {
            val dir = root ?: continue
            try {
                lock.withLock { refreshLocked(dir) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                gitToast(publish, e.message ?: "Failed to refresh repository")
            }
        }
    }

    val current: File? get() = root

    /** Points the worker at [path]; null or a non-repository closes it. Touches the disk, call off Main. */
    fun open(path: String?) {
        root = path?.let(::File)?.takeIf { it.resolve(".git").exists() }
    }

    fun requestRefresh() {
        refreshRequests.trySend(Unit)
    }

    fun run(op: GitOp) {
        scope.launch(Dispatchers.IO) {
            var success = false
            try {
                val dir = root
                if (dir == null) {
                    gitToast(publish, "Not a git repository")
                    return@launch
                }
                lock.withLock {
                    val pakkuBefore = pakkuFilesStamp(dir)
                    val failure = try {
                        runGitOp(publish, dir, op, latestModel().modpack.git)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        e.message ?: "${op.status.removeSuffix("…")} failed"
                    }
                    if (failure != null) gitToast(publish, failure) else success = true
                    try {
                        refreshLocked(dir)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        gitToast(publish, e.message ?: "Failed to refresh repository")
                    }
                    // Checkout, pull, stash and rollback rewrite the working tree. Like an IDE
                    // reloading open files, re-read the modpack when its files changed.
                    if (pakkuFilesStamp(dir) != pakkuBefore) reloadModpack()
                }
            } finally {
                withContext(NonCancellable) {
                    publish(AppMsg.Modpack(ModpackMsg.GitOpFinished(op, success)))
                }
            }
        }
    }

    private suspend fun refreshLocked(dir: File) {
        val state = NativeGit.readState(dir)
        // The user may have closed or switched the modpack while git ran.
        if (root == dir) publish(AppMsg.Modpack(ModpackMsg.GitStateUpdated(state)))
    }

    private suspend fun reloadModpack() {
        val model = latestModel()
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded || model.modpack.actionName != null) return
        val lockFile = LockFile.readToResult()
        val configFile = ConfigFile.readToResult()
        publish(AppMsg.Modpack(ModpackMsg.Loaded(lockFile, configFile)))
    }

    private fun pakkuFilesStamp(dir: File): List<Pair<Long, Long>> =
        listOf(LockFile.FILE_NAME, ConfigFile.FILE_NAME).map { name ->
            File(dir, name).let { it.lastModified() to it.length() }
        }
}

// -- Helpers --

private fun gitToast(publish: (AppMsg) -> Unit, message: String) {
    publish(AppMsg.Modpack(ModpackMsg.ToastAdded(actionInfoToast(message))))
}

// -- Native-git ops --

/** Regex that matches git's progress lines: "Counting objects:  73% (8/11)". */
private val GIT_PROGRESS_RE = Regex("""(?:remote:\s+)?(.+?):\s+\d+%\s+\((\d+)/(\d+)\)""")

/** Each progress message recomposes the whole window; 10 per second is plenty for a bar. */
private const val GIT_PROGRESS_PUBLISH_INTERVAL_MS = 100L

/** Failure text kept for the toast. Progress lines are not stored. */
private const val MAX_GIT_STDERR_CHARS = 64_000
private const val MAX_GIT_STDERR_LINE_CHARS = 8_000

/** "Counting objects:  73% (8/11)", with an optional "remote: " prefix. Null when [line] is not progress. */
private fun gitProgress(line: String): GitEvent.Progress? {
    val match = GIT_PROGRESS_RE.find(line) ?: return null
    val current = match.groupValues[2].toIntOrNull() ?: return null
    val total = match.groupValues[3].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return GitEvent.Progress(operation = match.groupValues[1].trim(), current = current, total = total)
}

/**
 * Runs a git command, publishes [GitEvent.Progress] from stderr, and returns the exit code
 * with the remaining stderr (capped) for the failure toast.
 */
private suspend fun runGitWithProgress(
    workDir: File,
    publish: (AppMsg) -> Unit,
    vararg args: String,
): NativeGit.Output = coroutineScope {
    val proc = NativeGit.command(workDir, args.toList(), readOnly = false).startOnIo()
    proc.outputStream.close()

    val stderr = StringBuilder()
    val (progress, progressJob) = conflatedPublisher<GitEvent.Progress?>(GIT_PROGRESS_PUBLISH_INTERVAL_MS) { event ->
        publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
    }

    val stderrJob = launch(Dispatchers.IO) {
        ignoreBrokenPipe {
            proc.errorStream.bufferedReader().use { reader ->
                reader.forEachLine(MAX_GIT_STDERR_LINE_CHARS) { raw ->
                    val line = raw.trim()
                    val event = gitProgress(line)
                    when {
                        event != null -> progress.trySend(event)
                        line.isNotEmpty() -> appendCappedStderr(stderr, line)
                    }
                }
            }
        }
    }
    val stdoutJob = launch(Dispatchers.IO) { ignoreBrokenPipe { proc.inputStream.discardFully() } }

    val exitCode = proc.awaitOrDestroy()
    stderrJob.join()
    stdoutJob.join()

    progress.trySend(null)
    progress.close()
    progressJob.join()

    NativeGit.Output(exitCode, stderr.toString())
}

/** A pipe broken because the process was destroyed is not a failure of the command. */
private suspend fun ignoreBrokenPipe(read: suspend () -> Unit) {
    try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
    }
}

private fun appendCappedStderr(capture: StringBuilder, line: String) {
    capture.append(line).append('\n')
    if (capture.length > MAX_GIT_STDERR_CHARS) {
        capture.delete(0, capture.length - MAX_GIT_STDERR_CHARS)
    }
}

private suspend fun runPush(publish: (AppMsg) -> Unit, workDir: File, state: GitState, force: Boolean, tags: Boolean): NativeGit.Output {
    val args = buildList {
        add("push")
        add("--progress")
        if (force) add("--force-with-lease")
        if (tags) add("--tags")
        if (state.upstream == null) {
            val remote = state.defaultRemote ?: return NativeGit.Output(1, "No remote configured")
            add("-u")
            add(remote)
            add("HEAD")
        }
    }
    return runGitWithProgress(workDir, publish, *args.toTypedArray())
}

private suspend fun runCheckout(workDir: File, branch: GitBranch): NativeGit.Output {
    if (!branch.isRemote) return NativeGit.exec(workDir, "checkout", branch.name)
    val localName = branch.name.substringAfter('/', missingDelimiterValue = branch.name)
    // Try a plain checkout first in case the local branch already exists.
    val existing = NativeGit.exec(workDir, "checkout", localName)
    return if (existing.ok) existing
    else NativeGit.exec(workDir, "checkout", "-b", localName, "--track", branch.name)
}

/** Runs [op]; returns null on success or the message to toast on failure. */
private suspend fun runGitOp(publish: (AppMsg) -> Unit, workDir: File, op: GitOp, state: GitState): String? {
    val result: NativeGit.Output = when (op) {
        GitOp.Refresh -> NativeGit.Output(0, "")
        GitOp.Fetch -> runGitWithProgress(workDir, publish, "fetch", "--all", "--prune", "--progress")
        is GitOp.Pull -> runGitWithProgress(
            workDir, publish,
            "pull", "--progress", if (op.rebase) "--rebase" else "--no-rebase",
        )
        is GitOp.Push -> runPush(publish, workDir, state, op.force, op.tags)
        is GitOp.Commit -> {
            val commit = NativeGit.commitPaths(workDir, op.message, op.paths, op.amend)
            if (commit.ok && op.pushAfter) {
                // Upstream may have changed meaning after the commit; re-read before pushing.
                runPush(publish, workDir, NativeGit.readState(workDir), force = op.amend, tags = false)
            } else commit
        }
        is GitOp.Checkout -> runCheckout(workDir, op.branch)
        is GitOp.CreateBranch -> {
            val args = buildList {
                if (op.checkout) addAll(listOf("checkout", "-b")) else add("branch")
                add(op.name)
                op.startPoint?.let(::add)
            }
            NativeGit.exec(workDir, *args.toTypedArray())
        }
        is GitOp.DeleteBranch -> NativeGit.exec(workDir, "branch", if (op.force) "-D" else "-d", op.branch.name)
        is GitOp.Rollback -> NativeGit.rollback(workDir, op.trackedPaths, op.addedPaths, op.deleteAdded)
        is GitOp.Stash -> {
            val args = buildList {
                addAll(listOf("stash", "push"))
                if (op.includeUntracked) add("--include-untracked")
                if (op.message.isNotBlank()) addAll(listOf("-m", op.message.trim()))
            }
            NativeGit.exec(workDir, *args.toTypedArray())
        }
        is GitOp.Unstash -> NativeGit.exec(workDir, "stash", if (op.pop) "pop" else "apply", op.stash.ref)
        is GitOp.DropStash -> NativeGit.exec(workDir, "stash", "drop", op.stash.ref)
        is GitOp.Ignore -> {
            NativeGit.ignore(workDir, op.paths)
            NativeGit.Output(0, "")
        }
        is GitOp.CheckoutRevision -> NativeGit.exec(workDir, "checkout", "--detach", op.hash)
        is GitOp.CherryPick -> NativeGit.exec(
            workDir,
            *buildList {
                add("cherry-pick")
                if (op.merge) addAll(listOf("-m", "1"))
                add(op.hash)
            }.toTypedArray(),
        )
        is GitOp.Revert -> NativeGit.exec(
            workDir,
            *buildList {
                add("revert")
                add("--no-edit")
                if (op.merge) addAll(listOf("-m", "1"))
                add(op.hash)
            }.toTypedArray(),
        )
        is GitOp.Reset -> NativeGit.exec(workDir, "reset", op.mode.flag, op.hash)
        is GitOp.CreateTag -> NativeGit.exec(workDir, "tag", op.name, op.hash)
        is GitOp.DeleteTag -> NativeGit.exec(workDir, "tag", "-d", op.name)
    }
    return if (result.ok) null else result.failure("${op.status.removeSuffix("…")} failed")
}

// -- Driver --

val gitDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestModel by rememberUpdatedState(model)
    val scope = rememberCoroutineScope()
    val worker = remember(scope) { GitRepositoryWorker(scope, publish) { latestModel } }
    val modpackOpen = model.screen == AppScreen.Modpack && model.modpack.loaded
    val modpackPath = model.profile.data.currentProfile?.path

    LaunchedEffect(worker) { worker.processRefreshes() }

    LaunchedEffect(modpackOpen, modpackPath) {
        val path = modpackPath.takeIf { modpackOpen }
        withContext(Dispatchers.IO) { worker.open(path) }
        if (path != null) worker.requestRefresh()
    }

    // Keyed on the op alone: closing a dialog or reloading the modpack must not restart it.
    LaunchedEffect(model.modpack.pendingGitOp) {
        val op = model.modpack.pendingGitOp ?: return@LaunchedEffect
        worker.run(op)
    }

    LaunchedEffect(model.modpack.gitDiffPendingFile) {
        val file = model.modpack.gitDiffPendingFile ?: return@LaunchedEffect
        val dir = worker.current ?: run {
            publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(null)))
            return@LaunchedEffect
        }
        try {
            val diff = withContext(Dispatchers.IO) { NativeGit.computeDiff(dir, file) }
            publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(diff)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Failed to compute diff")
            publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(null)))
        }
    }

    var changelistPrep by remember { mutableStateOf<GitChangelistPrep?>(null) }
    LaunchedEffect(model.screen, model.modpack.git.gitFiles, model.modpack.gitGroupUnversioned) {
        if (model.screen != AppScreen.Modpack) {
            changelistPrep = null
            return@LaunchedEffect
        }
        val files = model.modpack.git.gitFiles
        val grouped = model.modpack.gitGroupUnversioned
        val snapshot = withContext(Dispatchers.Default) { gitChangelistUiSnapshot(files, grouped) }
        changelistPrep = GitChangelistPrep(files, grouped, snapshot)
    }

    CompositionLocalProvider(LocalGitChangelistPrep provides changelistPrep) {
        content()
    }
}
