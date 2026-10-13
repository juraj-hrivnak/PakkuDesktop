/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
/**
 * Progress events surfaced from native git stderr into the ELM model.
 */
sealed class GitEvent {
    data class Progress(
        val operation: String,
        val current: Int,
        val total: Int? = null,
        val message: String? = null,
    ) : GitEvent() {
        val percentage: Float
            get() = total?.let { current.toFloat() / it } ?: 0f
    }
}

data class GitState(
    val gitFiles: PersistentList<GitFile> = persistentListOf(),
    val selectedFiles: PersistentSet<GitFile> = persistentSetOf(),
    /** Changelist tree folder ids that are expanded (UI only; see `gitTreePath`). */
    val expandedFolderPaths: PersistentSet<String> = persistentSetOf(),
    val branches: Set<GitBranch> = emptySet(),
    /** Commits on HEAD not on the upstream — or on no remote when there is no upstream. */
    val outgoingCommits: Set<GitCommit> = emptySet(),
    val commitMessage: String = "",
    /** Upstream of the current branch (`origin/main`); null when not tracking. */
    val upstream: String? = null,
    /** Commits on the upstream not yet on HEAD (as of the last fetch). */
    val incomingCount: Int = 0,
    val remotes: PersistentList<String> = persistentListOf(),
    /** Full message of HEAD, prefilled when amending. Empty before the first commit. */
    val headMessage: String = "",
    val stashes: PersistentList<GitStash> = persistentListOf(),
) {
    val currentBranch: GitBranch? get() = branches.firstOrNull { it.isCurrent }

    /** Remote that a branch without upstream is pushed to. */
    val defaultRemote: String? get() = remotes.firstOrNull { it == "origin" } ?: remotes.firstOrNull()
}

/** Modal Git dialogs; at most one is open ([teksturepako.pakkuDesktop.app.ui.model.ModpackModel.gitDialog]). */
sealed interface GitDialog {
    data object Commit : GitDialog
    /** Update Project (pull). */
    data object Update : GitDialog
    data object Push : GitDialog
    data object Rollback : GitDialog
    data class NewBranch(val startPoint: GitBranch? = null) : GitDialog
    data class DeleteBranch(val branch: GitBranch) : GitDialog
    data object Stash : GitDialog
    data object Unstash : GitDialog
}

/** One native git operation; set as `pendingGitOp` and run by the git driver. */
sealed interface GitOp {
    /** Footer / title-bar status while running. */
    val status: String

    data object Refresh : GitOp {
        override val status get() = "Refreshing…"
    }

    data object Fetch : GitOp {
        override val status get() = "Fetching…"
    }

    data class Pull(val rebase: Boolean) : GitOp {
        override val status get() = "Updating…"
    }

    data class Push(val force: Boolean = false) : GitOp {
        override val status get() = "Pushing…"
    }

    data class Commit(
        val message: String,
        val paths: List<String>,
        val amend: Boolean = false,
        val pushAfter: Boolean = false,
    ) : GitOp {
        override val status get() = if (pushAfter) "Committing and pushing…" else "Committing…"
    }

    data class Checkout(val branch: GitBranch) : GitOp {
        override val status get() = "Checking out ${branch.name}…"
    }

    data class CreateBranch(val name: String, val startPoint: String?, val checkout: Boolean) : GitOp {
        override val status get() = "Creating $name…"
    }

    data class DeleteBranch(val branch: GitBranch, val force: Boolean) : GitOp {
        override val status get() = "Deleting ${branch.name}…"
    }

    /** Tracked paths go back to HEAD; [addedPaths] are unstaged (and deleted with [deleteAdded]). */
    data class Rollback(
        val trackedPaths: List<String>,
        val addedPaths: List<String>,
        val deleteAdded: Boolean,
    ) : GitOp {
        override val status get() = "Rolling back…"
    }

    data class Stash(val message: String, val includeUntracked: Boolean) : GitOp {
        override val status get() = "Stashing…"
    }

    data class Unstash(val stash: GitStash, val pop: Boolean) : GitOp {
        override val status get() = "Unstashing…"
    }

    data class DropStash(val stash: GitStash) : GitOp {
        override val status get() = "Dropping ${stash.ref}…"
    }

    /** Appends repo-relative [paths] to the root `.gitignore`. */
    data class Ignore(val paths: List<String>) : GitOp {
        override val status get() = "Updating .gitignore…"
    }
}
