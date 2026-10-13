/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.actions.actionInfoToast
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.PROGRESS_PUBLISH_INTERVAL_MS
import teksturepako.pakkuDesktop.elm.conflatedPublisher
import teksturepako.pakkuDesktop.app.git.GitBranch
import teksturepako.pakkuDesktop.app.git.GitEvent
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.git.GitState
import teksturepako.pakkuDesktop.app.git.NativeGit
import teksturepako.pakkuDesktop.app.git.awaitOrDestroy
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

// -- Repository handle --

private class GitRepoHolder {
    var path: String? = null
    var workDir: File? = null

    fun open(workingPathStr: String) {
        if (path == workingPathStr && workDir != null) return
        close()
        val dir = File(workingPathStr)
        if (dir.resolve(".git").exists()) {
            workDir = dir
            path = workingPathStr
        }
    }

    fun close() {
        workDir = null
        path = null
    }
}

// -- Helpers --

private suspend fun gitToast(publish: (AppMsg) -> Unit, message: String) {
    withContext(Dispatchers.Main) {
        publish(AppMsg.Modpack(ModpackMsg.ToastAdded(actionInfoToast(message))))
    }
}

private suspend fun refreshGitState(publish: (AppMsg) -> Unit, workDir: File, preserve: GitState) {
    val newState = NativeGit.buildState(workDir, preserve)
    withContext(Dispatchers.Main) {
        publish(AppMsg.Modpack(ModpackMsg.GitStateUpdated(newState)))
    }
}

// -- Native-git ops --

/** Regex that matches git's progress lines: "Counting objects:  73% (8/11)". */
private val GIT_PROGRESS_RE = Regex("""(?:remote:\s+)?(.+?):\s+\d+%\s+\((\d+)/(\d+)\)""")

/**
 * Runs a git command, streams stderr line-by-line to publish [GitEvent.Progress] events,
 * and returns the exit code with captured stderr.
 */
private suspend fun runGitWithProgress(
    workDir: File,
    publish: (AppMsg) -> Unit,
    vararg args: String,
): NativeGit.Output = coroutineScope {
    val proc = ProcessBuilder("git", *args)
        .directory(workDir)
        .start()
    proc.outputStream.close()

    val stderrCapture = StringBuilder()
    val (gitProgressCh, gitProgressJob) = conflatedPublisher<GitEvent.Progress?>(PROGRESS_PUBLISH_INTERVAL_MS) { event ->
        withContext(Dispatchers.Main) {
            publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
        }
    }

    // Read stderr char-by-char so we catch both \n and \r progress updates.
    val stderrJob = launch(Dispatchers.IO) {
        val sb = StringBuilder()
        try {
            proc.errorStream.bufferedReader().use { reader ->
                var ch: Int
                while (reader.read().also { ch = it } != -1) {
                    val c = ch.toChar()
                    if (c == '\r' || c == '\n') {
                        val line = sb.toString().trim()
                        sb.clear()
                        if (line.isNotBlank()) {
                            val m = GIT_PROGRESS_RE.find(line)
                            if (m != null) {
                                val current = m.groupValues[2].toIntOrNull()
                                val total   = m.groupValues[3].toIntOrNull()
                                if (current != null && total != null && total > 0) {
                                    val event = GitEvent.Progress(
                                        operation = m.groupValues[1].trim(),
                                        current = current,
                                        total = total,
                                    )
                                    gitProgressCh.trySend(event)
                                }
                            } else {
                                stderrCapture.append(line).append('\n')
                            }
                        }
                    } else {
                        sb.append(c)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Drain stdout to prevent the process from blocking on a full buffer.
    val stdoutJob = launch(Dispatchers.IO) {
        proc.inputStream.bufferedReader().use { it.readText() }
    }

    val exitCode = proc.awaitOrDestroy()
    stderrJob.join()
    stdoutJob.join()

    gitProgressCh.trySend(null)
    gitProgressCh.close()
    gitProgressJob.join()

    NativeGit.Output(exitCode, stderrCapture.toString())
}

private suspend fun runPush(publish: (AppMsg) -> Unit, workDir: File, state: GitState, force: Boolean): NativeGit.Output {
    val args = buildList {
        add("push")
        add("--progress")
        if (force) add("--force-with-lease")
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
        is GitOp.Push -> runPush(publish, workDir, state, op.force)
        is GitOp.Commit -> {
            val commit = NativeGit.commitPaths(workDir, op.message, op.paths, op.amend)
            if (commit.ok && op.pushAfter) {
                // Upstream may have changed meaning after the commit; re-read before pushing.
                runPush(publish, workDir, NativeGit.buildState(workDir, state), force = op.amend)
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
    }
    return if (result.ok) null else result.failure("${op.status.removeSuffix("…")} failed")
}

// -- Driver --

val gitDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestModel by rememberUpdatedState(model)
    val holder = remember { GitRepoHolder() }
    val modpackPath = model.profile.data.currentProfile?.path

    LaunchedEffect(model.screen, model.modpack.loaded, modpackPath) {
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) {
            holder.close()
            return@LaunchedEffect
        }
        val path = modpackPath ?: run {
            holder.close()
            return@LaunchedEffect
        }
        try {
            holder.open(path)
            val dir = holder.workDir ?: return@LaunchedEffect
            val preserve = latestModel.modpack.git
            withContext(Dispatchers.IO) {
                refreshGitState(publish, dir, preserve)
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Failed to open repository")
        }
    }

    LaunchedEffect(model.modpack.gitDiffPendingFile, model.screen, model.modpack.loaded, modpackPath) {
        val file = model.modpack.gitDiffPendingFile ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val path = modpackPath ?: return@LaunchedEffect
        try {
            holder.open(path)
            val dir = holder.workDir ?: run {
                publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(null)))
                return@LaunchedEffect
            }
            val diff = withContext(Dispatchers.IO) { NativeGit.computeDiff(dir, file) }
            publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(diff)))
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Failed to compute diff")
            publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(null)))
        }
    }

    LaunchedEffect(model.modpack.pendingGitOp, model.screen, model.modpack.loaded) {
        val op = model.modpack.pendingGitOp ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val dir = holder.workDir ?: run {
            publish(AppMsg.Modpack(ModpackMsg.GitOpFinished(op, success = false)))
            return@LaunchedEffect
        }
        val failure = try {
            withContext(Dispatchers.IO) { runGitOp(publish, dir, op, latestModel.modpack.git) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "${op.status.removeSuffix("…")} failed"
        }
        if (failure != null) gitToast(publish, failure)
        try {
            withContext(Dispatchers.IO) { refreshGitState(publish, dir, latestModel.modpack.git) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Failed to refresh repository")
        }
        publish(AppMsg.Modpack(ModpackMsg.GitOpFinished(op, success = failure == null)))
    }

    content()
}
