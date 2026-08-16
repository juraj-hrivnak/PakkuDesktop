/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.pro.git.wrapper.GitEvent
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState
import teksturepako.pakkuDesktop.pro.git.wrapper.NativeGit
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import java.io.File

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
        publish(
            AppMsg.Modpack(
                ModpackMsg.ToastAdded(
                    ToastData {
                        Box(modifier = Modifier.padding(16.dp).width(300.dp)) { Text(message) }
                    },
                ),
            ),
        )
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
 * and returns (exitCode, captured stderr).
 */
private suspend fun runGitWithProgress(
    workDir: File,
    publish: (AppMsg) -> Unit,
    vararg args: String,
): Pair<Int, String> = coroutineScope {
    val proc = ProcessBuilder("git", *args)
        .directory(workDir)
        .start()

    val stderrCapture = StringBuilder()

    // Read stderr char-by-char so we catch both \n and \r progress updates.
    val stderrJob = launch(Dispatchers.IO) {
        val sb = StringBuilder()
        try {
            val reader = proc.errorStream.bufferedReader()
            var ch: Int
            while (reader.read().also { ch = it } != -1) {
                val c = ch.toChar()
                if (c == '\r' || c == '\n') {
                    val line = sb.toString().trim()
                    sb.clear()
                    if (line.isNotBlank()) {
                        stderrCapture.append(line).append('\n')
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
                                withContext(Dispatchers.Main) {
                                    publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
                                }
                            }
                        }
                    }
                } else {
                    sb.append(c)
                }
            }
        } catch (_: Exception) {}
    }

    // Drain stdout to prevent the process from blocking on a full buffer.
    val stdoutJob = launch(Dispatchers.IO) {
        proc.inputStream.bufferedReader().readText()
    }

    val exitCode = proc.waitFor()
    stderrJob.join()
    stdoutJob.join()

    // Clear any lingering progress indicator.
    withContext(Dispatchers.Main) {
        publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(null)))
    }

    exitCode to stderrCapture.toString()
}

private suspend fun runPull(publish: (AppMsg) -> Unit, workDir: File) {
    try {
        val (exitCode, stderr) = runGitWithProgress(workDir, publish, "pull", "--progress")
        if (exitCode != 0) {
            gitToast(publish, stderr.trim().lines().lastOrNull { it.isNotBlank() } ?: "Pull failed")
        }
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Pull failed")
    }
}

private suspend fun runPush(publish: (AppMsg) -> Unit, workDir: File) {
    try {
        val (exitCode, stderr) = runGitWithProgress(workDir, publish, "push", "--progress")
        if (exitCode != 0) {
            gitToast(publish, stderr.trim().lines().lastOrNull { it.isNotBlank() } ?: "Push failed")
        }
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Push failed")
    }
}

private suspend fun runCommit(publish: (AppMsg) -> Unit, workDir: File, gitState: GitState): Boolean {
    if (gitState.commitMessage.isBlank()) return false
    return try {
        // Stage each selected file individually.
        for (file in gitState.selectedFiles) {
            ProcessBuilder("git", "add", "--", file.path)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        }
        val proc = ProcessBuilder("git", "commit", "-m", gitState.commitMessage)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            // Roll back staged changes on failure.
            ProcessBuilder("git", "reset", "--mixed", "HEAD")
                .directory(workDir).redirectErrorStream(true).start().waitFor()
            gitToast(publish, output.trim().ifEmpty { "Commit failed" })
        }
        exitCode == 0
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Commit failed")
        false
    }
}

private suspend fun runCheckout(publish: (AppMsg) -> Unit, workDir: File, branch: GitBranch) {
    try {
        val exitCode = if (branch.isRemote) {
            val localName = branch.name.substringAfter('/', missingDelimiterValue = branch.name)
            // Try a plain checkout first in case the local branch already exists.
            val first = ProcessBuilder("git", "checkout", localName)
                .directory(workDir).redirectErrorStream(true).start().waitFor()
            if (first != 0) {
                // Local branch doesn't exist yet — create it with remote tracking.
                ProcessBuilder("git", "checkout", "-b", localName, "--track", branch.name)
                    .directory(workDir).redirectErrorStream(true).start().waitFor()
            } else first
        } else {
            ProcessBuilder("git", "checkout", branch.name)
                .directory(workDir).redirectErrorStream(true).start().waitFor()
        }
        if (exitCode != 0) gitToast(publish, "Checkout failed")
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Checkout failed")
    }
}

// -- Driver --

val gitDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val holder = remember { GitRepoHolder() }

    LaunchedEffect(model.screen, model.modpack.loaded, workingPath) {
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) {
            holder.close()
            return@LaunchedEffect
        }
        try {
            holder.open(workingPath)
            val dir = holder.workDir ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                refreshGitState(publish, dir, model.modpack.git)
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Failed to open repository")
        }
    }

    LaunchedEffect(model.modpack.gitDiffPendingFile, model.screen, model.modpack.loaded, workingPath) {
        val file = model.modpack.gitDiffPendingFile ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        try {
            holder.open(workingPath)
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

    LaunchedEffect(model.modpack.wantsGitPull, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitPull) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val dir = holder.workDir ?: return@LaunchedEffect
        val preserve = model.modpack.git
        try {
            withContext(Dispatchers.IO) {
                runPull(publish, dir)
                refreshGitState(publish, dir, preserve)
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Pull failed")
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPullFinished))
    }

    LaunchedEffect(model.modpack.wantsGitPush, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitPush) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val dir = holder.workDir ?: return@LaunchedEffect
        val preserve = model.modpack.git
        try {
            withContext(Dispatchers.IO) {
                runPush(publish, dir)
                refreshGitState(publish, dir, preserve)
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Push failed")
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPushFinished))
    }

    LaunchedEffect(model.modpack.wantsGitCommit, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitCommit) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val dir = holder.workDir ?: return@LaunchedEffect
        val preserve = model.modpack.git
        val commitOk = try {
            withContext(Dispatchers.IO) {
                val ok = runCommit(publish, dir, preserve)
                refreshGitState(publish, dir, preserve)
                ok
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Commit failed")
            false
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCommitFinished(commitOk)))
    }

    LaunchedEffect(model.modpack.gitCheckoutBranch, model.screen, model.modpack.loaded) {
        val branch = model.modpack.gitCheckoutBranch ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val dir = holder.workDir ?: return@LaunchedEffect
        val preserve = model.modpack.git
        try {
            withContext(Dispatchers.IO) {
                runCheckout(publish, dir, branch)
                refreshGitState(publish, dir, preserve)
            }
        } catch (e: Exception) {
            gitToast(publish, e.message ?: "Checkout failed")
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCheckoutFinished))
    }

    content()
}
