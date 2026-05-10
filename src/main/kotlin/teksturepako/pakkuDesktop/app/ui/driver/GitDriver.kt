/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.pro.git.GitDiffComputer
import teksturepako.pakkuDesktop.pro.git.GitEvent
import teksturepako.pakkuDesktop.pro.git.GitState
import teksturepako.pakkuDesktop.pro.git.buildGitState
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import kotlin.io.path.Path
import kotlinx.coroutines.CoroutineScope

private class GitRepoHolder {
    var path: String? = null
    var git: Git? = null
    var repository: Repository? = null

    fun open(workingPathStr: String) {
        if (path == workingPathStr && git != null) return
        close()
        GitDiffComputer.openRepository(Path(workingPathStr)).onSuccess { (g, r) ->
            git = g
            repository = r
            path = workingPathStr
        }
    }

    fun close() {
        git?.close()
        repository?.close()
        git = null
        repository = null
        path = null
    }
}

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

private fun jgitProgressMonitor(scope: CoroutineScope, publish: (AppMsg) -> Unit): ProgressMonitor {
    var task = ""
    var total = 0
    var work = 0
    return object : ProgressMonitor {
        override fun start(totalTasks: Int) {}
        override fun beginTask(title: String, totalWork: Int) {
            task = title
            total = totalWork
            work = 0
        }

        override fun update(completed: Int) {
            work += completed
            if (total <= 0) return
            val p = GitEvent.Progress(
                operation = task,
                current = work.coerceAtMost(total),
                total = total,
                message = null,
            )
            scope.launch {
                publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(p)))
            }
        }

        override fun endTask() {
            scope.launch {
                publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(null)))
            }
        }

        override fun isCancelled() = false
    }
}

private suspend fun refreshGitState(
    publish: (AppMsg) -> Unit,
    git: Git,
    repository: Repository,
    preserve: GitState,
) {
    val newState = buildGitState(git, repository, preserve)
    withContext(Dispatchers.Main) {
        publish(AppMsg.Modpack(ModpackMsg.GitStateUpdated(newState)))
    }
}

private suspend fun runPull(scope: CoroutineScope, publish: (AppMsg) -> Unit, git: Git) {
    try {
        git.pull()
            .setProgressMonitor(jgitProgressMonitor(scope, publish))
            .call()
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Pull failed")
    }
}

private suspend fun runPush(scope: CoroutineScope, publish: (AppMsg) -> Unit, git: Git) {
    try {
        git.push()
            .setProgressMonitor(jgitProgressMonitor(scope, publish))
            .call()
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Push failed")
    }
}

private suspend fun runCommit(publish: (AppMsg) -> Unit, git: Git, gitState: GitState): Boolean {
    if (gitState.commitMessage.isBlank()) return false
    return try {
        for (file in gitState.selectedFiles) {
            git.add().addFilepattern(file.path).call()
        }
        git.commit().setMessage(gitState.commitMessage).call()
        true
    } catch (e: Exception) {
        runCatching {
            git.reset().setMode(ResetCommand.ResetType.MIXED).call()
        }
        gitToast(publish, e.message ?: "Commit failed")
        false
    }
}

private suspend fun runCheckout(publish: (AppMsg) -> Unit, git: Git, branch: GitBranch) {
    try {
        if (branch.isRemote) {
            val localName = branch.name.substringAfter('/', missingDelimiterValue = branch.name)
            git.checkout()
                .setCreateBranch(true)
                .setName(localName)
                .setStartPoint(branch.name)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call()
        } else {
            git.checkout().setName(branch.name).call()
        }
    } catch (e: Exception) {
        gitToast(publish, e.message ?: "Checkout failed")
    }
}

val gitDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val holder = remember { GitRepoHolder() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(model.screen, model.modpack.loaded, workingPath) {
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) {
            holder.close()
            return@LaunchedEffect
        }
        holder.open(workingPath)
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            refreshGitState(publish, g, r, model.modpack.git)
        }
    }

    LaunchedEffect(model.modpack.gitDiffPendingFile, model.screen, model.modpack.loaded) {
        val file = model.modpack.gitDiffPendingFile ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        val diff = withContext(Dispatchers.IO) {
            GitDiffComputer.computeDiff(g, r, file)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitDiffComputed(diff)))
    }

    LaunchedEffect(model.modpack.wantsGitPull, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitPull) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runPull(scope, publish, g)
            refreshGitState(publish, g, r, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPullFinished))
    }

    LaunchedEffect(model.modpack.wantsGitPush, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitPush) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runPush(scope, publish, g)
            refreshGitState(publish, g, r, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPushFinished))
    }

    LaunchedEffect(model.modpack.wantsGitCommit, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitCommit) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        val preserve = model.modpack.git
        val commitOk = withContext(Dispatchers.IO) {
            val ok = runCommit(publish, g, preserve)
            refreshGitState(publish, g, r, preserve)
            ok
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCommitFinished(commitOk)))
    }

    LaunchedEffect(model.modpack.gitCheckoutBranch, model.screen, model.modpack.loaded) {
        val branch = model.modpack.gitCheckoutBranch ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val g = holder.git ?: return@LaunchedEffect
        val r = holder.repository ?: return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runCheckout(publish, g, branch)
            refreshGitState(publish, g, r, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCheckoutFinished))
    }

    content()
}
