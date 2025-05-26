package teksturepako.pakkuDesktop.pro.ui.viewmodel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.pro.git.*
import teksturepako.pakkuDesktop.pkui.component.toast.showToast
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitCommit
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

data class GitState(
    val gitFiles: List<GitFile> = listOf(),
    val selectedFiles: Set<GitFile> = setOf(),
    val branches: Set<GitBranch> = setOf(),
    val outgoingCommits: Set<GitCommit> = setOf(),
    val commitMessage: String = "",
)

object GitViewModel
{
    private val _gitState = MutableStateFlow(GitState())
    val gitState = _gitState.asStateFlow()

    private val _eventProgress = MutableStateFlow<GitEvent.Progress?>(null)
    val eventProgress = _eventProgress.asStateFlow()

    suspend fun load()
    {
        getGitBranches()
        getOutgoingCommits()
        getGitFiles()
    }

    // -- GIT IMPLEMENTATION --

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun getGitBranches()
    {
        val branches = (gitRepoOf(workingPath) exec "branch -a")
            .toList()
            .mapNotNull { result ->
                result.get()?.output()?.message
            }
            .let { parseGitBranches(it) }
            .toSet()

        _gitState.update { it.copy(branches = branches) }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun getOutgoingCommits()
    {
        val currentBranch = _gitState.value.branches.firstOrNull { it.isCurrent }?.name

        val outgoingCommits = (gitRepoOf(workingPath) exec "log $currentBranch --not --remotes --oneline")
            .toList()
            .mapNotNull { result ->
                result.get()?.output()?.message
            }
            .let { parseCommitLog(it) }
            .toSet()

        _gitState.update { it.copy(outgoingCommits = outgoingCommits) }
    }

    suspend fun getGitFiles()
    {
        (gitRepoOf(workingPath) exec "status --porcelain")
            .mapNotNull { result ->
                result.get()?.output()?.message
            }
            .toList()
            .let { lines ->
                val files = parsePorcelainStatus(lines)
                _gitState.update { it.copy(gitFiles = files) }
            }
    }

    // -- COMMANDS --

    suspend fun checkout(branch: GitBranch)
    {
        val remoteBranchName by lazy {
            branch.name.split('/', limit = 2).getOrNull(1)
        }

        if (branch.isRemote && remoteBranchName != null)
        {
            (gitRepoOf(workingPath) exec "checkout -b $remoteBranchName ${branch.name} --")
        }
        else
        {
            (gitRepoOf(workingPath) exec "checkout ${branch.name}")
        }.output(
            success = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            failure = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        )

        load()

        _eventProgress.update { null }
    }

    suspend fun pull()
    {
        val currentBranch = _gitState.value.branches.firstOrNull { it.isCurrent }?.name

        val (remoteName, remoteBranch) = _gitState.value.branches
            .filter { it.isRemote }
            .map { remote ->
                remote.name
                    .split('/', limit = 2)
                    .let {
                        val remoteName = it.getOrNull(0) ?: return
                        val remoteBranch = it.getOrNull(1) ?: return

                        remoteName to remoteBranch
                    }
            }
            .find { (_, remoteBranch) ->
                currentBranch == remoteBranch
            }
            ?: return

        if (remoteBranch != currentBranch) return

        (gitRepoOf(workingPath) exec "fetch $remoteName --progress").output(
            progress = { event ->
                _eventProgress.update { event }
            },
            success = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            failure = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        )

        (gitRepoOf(workingPath) exec "pull $remoteName $remoteBranch --progress").output(
            progress = { event ->
                _eventProgress.update { event }
            },
            success = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            failure = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        )

        load()

        _eventProgress.update { null }
    }

    suspend fun push()
    {
        (gitRepoOf(workingPath) exec "push --progress origin HEAD").output(
            progress = { event ->
                _eventProgress.update { event }
            },
            success = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            failure = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        )

        load()

        _eventProgress.update { null }
    }

    suspend fun commit()
    {
        if (_gitState.value.commitMessage.isBlank()) return

        val addFilesResult = _gitState.value.selectedFiles.flatMap { file ->
            (gitRepoOf(workingPath) exec "add \"${file.path}\"").mapNotNull { it.getError() }.toList()
        }

        if (addFilesResult.isNotEmpty())
        {
            withContext(Dispatchers.Main) {
                ModpackViewModel.toasts.showToast {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .width(300.dp)
                    ) {
                        addFilesResult.map {
                            Text(it.message)
                        }
                    }
                }
            }
            (gitRepoOf(workingPath) exec "reset")
            return
        }

        (gitRepoOf(workingPath) exec "commit -m \"${_gitState.value.commitMessage}\"").output(
            success = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            },
            failure = {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        )

        load()

        _eventProgress.update { null }
    }

    // -- STATE --

    fun toggleFileSelection(gitFile: GitFile) = _gitState.update { currentState ->
        val newSelectedFiles = if (gitFile !in currentState.selectedFiles)
        {
            currentState.selectedFiles + gitFile
        }
        else
        {
            currentState.selectedFiles - gitFile
        }

        currentState.copy(
            selectedFiles = newSelectedFiles,
        )
    }

    fun updateCommitMessage(updatedMessage: String) = _gitState.update { currentState ->
        currentState.copy(
            commitMessage = updatedMessage
        )
    }
}