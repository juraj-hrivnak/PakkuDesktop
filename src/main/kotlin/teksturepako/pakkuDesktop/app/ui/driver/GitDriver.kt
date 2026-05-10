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
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.pro.git.GitError
import teksturepako.pakkuDesktop.pro.git.GitEvent
import org.eclipse.jgit.api.Git
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
import teksturepako.pakkuDesktop.pro.git.GitState
import teksturepako.pakkuDesktop.pro.git.exec
import teksturepako.pakkuDesktop.pro.git.gitRepoOf
import teksturepako.pakkuDesktop.pro.git.output
import teksturepako.pakkuDesktop.pro.git.parseCommitLog
import teksturepako.pakkuDesktop.pro.git.parseGitBranches
import teksturepako.pakkuDesktop.pro.git.parsePorcelainStatus
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import kotlin.io.path.Path

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

private suspend fun refreshGitState(publish: (AppMsg) -> Unit, preserve: GitState) {
    val branches = (gitRepoOf(workingPath) exec "branch -a")
        .toList()
        .mapNotNull { line: com.github.michaelbull.result.Result<GitEvent, GitError> ->
            line.fold(
                success = { event -> event.output()?.message },
                failure = { null },
            )
        }
        .let { parseGitBranches(it) }
        .toSet()

    val currentBranch = branches.firstOrNull { it.isCurrent }?.name
    val outgoingCommits = (gitRepoOf(workingPath) exec "log $currentBranch --not --remotes --oneline")
        .toList()
        .mapNotNull { line: com.github.michaelbull.result.Result<GitEvent, GitError> ->
            line.fold(
                success = { event -> event.output()?.message },
                failure = { null },
            )
        }
        .let { parseCommitLog(it) }
        .toSet()

    val gitFiles = (gitRepoOf(workingPath) exec "status --porcelain")
        .toList()
        .mapNotNull { line: com.github.michaelbull.result.Result<GitEvent, GitError> ->
            line.fold(
                success = { event -> event.output()?.message },
                failure = { null },
            )
        }
        .let { parsePorcelainStatus(it) }

    val newState = preserve.copy(
        branches = branches,
        outgoingCommits = outgoingCommits,
        gitFiles = gitFiles,
    )
    withContext(Dispatchers.Main) {
        publish(AppMsg.Modpack(ModpackMsg.GitStateUpdated(newState)))
    }
}

private suspend fun runPull(publish: (AppMsg) -> Unit, gitState: GitState) {
    val currentBranch = gitState.branches.firstOrNull { it.isCurrent }?.name
    val (remoteName, remoteBranch) = gitState.branches
        .filter { it.isRemote }
        .mapNotNull { remote ->
            remote.name.split('/', limit = 2).let {
                val rn = it.getOrNull(0) ?: return@mapNotNull null
                val rb = it.getOrNull(1) ?: return@mapNotNull null
                rn to rb
            }
        }
        .find { (_, rb) -> currentBranch == rb }
        ?: return

    if (remoteBranch != currentBranch) return

    (gitRepoOf(workingPath) exec "fetch $remoteName --progress").output(
        progress = { event ->
            publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
        },
        success = { gitToast(publish, it) },
        failure = { gitToast(publish, it) },
    )

    (gitRepoOf(workingPath) exec "pull $remoteName $remoteBranch --progress").output(
        progress = { event ->
            publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
        },
        success = { gitToast(publish, it) },
        failure = { gitToast(publish, it) },
    )
}

private suspend fun runPush(publish: (AppMsg) -> Unit) {
    (gitRepoOf(workingPath) exec "push --progress origin HEAD").output(
        progress = { event ->
            publish(AppMsg.Modpack(ModpackMsg.GitEventProgressUpdated(event)))
        },
        success = { gitToast(publish, it) },
        failure = { gitToast(publish, it) },
    )
}

private suspend fun runCommit(publish: (AppMsg) -> Unit, gitState: GitState) {
    if (gitState.commitMessage.isBlank()) return

    val addFilesResult = gitState.selectedFiles.flatMap { file ->
        (gitRepoOf(workingPath) exec "add \"${file.path}\"")
            .mapNotNull { line -> line.getError() }
            .toList()
    }

    if (addFilesResult.isNotEmpty()) {
        withContext(Dispatchers.Main) {
            publish(
                AppMsg.Modpack(
                    ModpackMsg.ToastAdded(
                        ToastData {
                            Box(modifier = Modifier.padding(16.dp).width(300.dp)) {
                                addFilesResult.forEach { Text(it.message) }
                            }
                        },
                    ),
                ),
            )
        }
        (gitRepoOf(workingPath) exec "reset").toList()
        return
    }

    (gitRepoOf(workingPath) exec "commit -m \"${gitState.commitMessage}\"").output(
        success = { gitToast(publish, it) },
        failure = { gitToast(publish, it) },
    )
}

private suspend fun runCheckout(publish: (AppMsg) -> Unit, branch: GitBranch) {
    val remoteBranchName by lazy { branch.name.split('/', limit = 2).getOrNull(1) }

    if (branch.isRemote && remoteBranchName != null) {
        (gitRepoOf(workingPath) exec "checkout -b $remoteBranchName ${branch.name} --")
    } else {
        (gitRepoOf(workingPath) exec "checkout ${branch.name}")
    }.output(
        success = { gitToast(publish, it) },
        failure = { gitToast(publish, it) },
    )
}

val gitDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val holder = remember { GitRepoHolder() }

    LaunchedEffect(model.screen, model.modpack.loaded, workingPath) {
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) {
            holder.close()
            return@LaunchedEffect
        }
        holder.open(workingPath)
        withContext(Dispatchers.IO) {
            refreshGitState(publish, model.modpack.git)
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
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runPull(publish, preserve)
            refreshGitState(publish, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPullFinished))
    }

    LaunchedEffect(model.modpack.wantsGitPush, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitPush) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runPush(publish)
            refreshGitState(publish, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitPushFinished))
    }

    LaunchedEffect(model.modpack.wantsGitCommit, model.screen, model.modpack.loaded) {
        if (!model.modpack.wantsGitCommit) return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runCommit(publish, preserve)
            refreshGitState(publish, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCommitFinished))
    }

    LaunchedEffect(model.modpack.gitCheckoutBranch, model.screen, model.modpack.loaded) {
        val branch = model.modpack.gitCheckoutBranch ?: return@LaunchedEffect
        if (model.screen != AppScreen.Modpack || !model.modpack.loaded) return@LaunchedEffect
        val preserve = model.modpack.git
        withContext(Dispatchers.IO) {
            runCheckout(publish, branch)
            refreshGitState(publish, preserve)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitCheckoutFinished))
    }

    content()
}
