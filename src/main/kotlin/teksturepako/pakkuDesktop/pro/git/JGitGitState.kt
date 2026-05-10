/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitCommit
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import java.io.File

/**
 * Builds [GitState] purely from JGit (no porcelain / CLI parsing). Call from an IO dispatcher.
 */
internal fun buildGitState(git: Git, repository: Repository, preserve: GitState): GitState {
    val status = git.status().call()
    val pathToChange = linkedMapOf<String, GitChange>()

    fun putAll(paths: Set<String>, factory: (String) -> GitChange) {
        for (p in paths) pathToChange[p] = factory(p)
    }

    putAll(status.added) { GitChange.Added(it) }
    putAll(status.changed) { GitChange.Modified(it) }
    putAll(status.modified) { GitChange.Modified(it) }
    putAll(status.removed) { GitChange.Deleted(it) }
    putAll(status.missing) { GitChange.Deleted(it) }
    putAll(status.untracked) { GitChange.Untracked(it) }
    putAll(status.conflicting) { GitChange.Modified(it) }

    val gitFiles = pathToChange.entries
        .sortedBy { it.key }
        .map { (path, change) -> gitFileForPath(repository, path, change) }

    return preserve.copy(
        branches = readBranches(repository, git),
        outgoingCommits = readOutgoingCommits(repository),
        gitFiles = gitFiles,
    )
}

private fun gitFileForPath(repository: Repository, path: String, status: GitChange): GitFile {
    val file = File(repository.workTree, path)
    return GitFile.fromFileInfo(
        path = path.replace(File.separatorChar, '/'),
        status = status,
        modifiedEpochMillis = file.lastModified(),
        size = file.length(),
    )
}

private fun readBranches(repository: Repository, git: Git): Set<GitBranch> {
    val full = repository.fullBranch
    val refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
    return refs.map { ref ->
        val name = when {
            ref.name.startsWith(Constants.R_HEADS) ->
                ref.name.removePrefix(Constants.R_HEADS)
            ref.name.startsWith(Constants.R_REMOTES) ->
                ref.name.removePrefix(Constants.R_REMOTES)
            else -> ref.name
        }
        GitBranch(
            name = name,
            isRemote = ref.name.startsWith(Constants.R_REMOTES),
            isCurrent = ref.name == full,
        )
    }.toSet()
}

private fun readOutgoingCommits(repository: Repository): Set<GitCommit> {
    val branch = repository.branch ?: return emptySet()
    val tracking = runCatching { BranchTrackingStatus.of(repository, branch) }.getOrNull()
        ?: return emptySet()
    val upstreamName = tracking.remoteTrackingBranch ?: return emptySet()
    val upstreamId = repository.resolve(upstreamName) ?: return emptySet()
    val headId = repository.resolve(Constants.HEAD) ?: return emptySet()
    if (upstreamId == headId) return emptySet()

    val walk = RevWalk(repository)
    return try {
        walk.markStart(walk.parseCommit(headId))
        walk.markUninteresting(walk.parseCommit(upstreamId))
        val out = LinkedHashSet<GitCommit>()
        while (true) {
            val c = walk.next() ?: break
            out += GitCommit(c.name.take(7), c.shortMessage)
            if (out.size >= 100) break
        }
        out
    } catch (_: Exception) {
        emptySet()
    } finally {
        walk.close()
    }
}
