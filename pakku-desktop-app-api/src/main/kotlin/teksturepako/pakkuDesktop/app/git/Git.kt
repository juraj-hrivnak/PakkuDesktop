/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/** Branch `git init` uses when `init.defaultBranch` is unset. */
const val DEFAULT_BRANCH_NAME = "main"

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
    /** Files changed on the upstream since it diverged from HEAD. Used to warn about conflicts. */
    val incomingChangedFiles: Set<String> = emptySet(),
    val remotes: PersistentList<String> = persistentListOf(),
    /** Full message of HEAD, prefilled when amending. Empty before the first commit. */
    val headMessage: String = "",
    val stashes: PersistentList<GitStash> = persistentListOf(),
    /** Branch and tag tips; the Log reloads its graph when these or [headHash] change. */
    val refs: PersistentList<GitRef> = persistentListOf(),
    /** Tag names on the default remote (from `git ls-remote --tags`); used to filter the Push dialog. */
    val remoteTagNames: Set<String> = emptySet(),
    /** Commit at HEAD; null before the first commit. */
    val headHash: String? = null,
    /**
     * null until the open folder has been checked.
     * false → the Git menu offers [GitDialog.Init].
     */
    val isRepository: Boolean? = null,
    /** Prefill for [GitDialog.Init]. Set when [isRepository] is false. */
    val defaultBranch: String = DEFAULT_BRANCH_NAME,
) {
    val currentBranch: GitBranch? get() = branches.firstOrNull { it.isCurrent }

    /** Remote that a branch without upstream is pushed to. */
    val defaultRemote: String? get() = remotes.firstOrNull { it == "origin" } ?: remotes.firstOrNull()
}

/**
 * UI state of the Git Log. The commit graph itself is not here: the log driver builds it off the
 * UI thread and provides it to the view.
 */
data class GitLogModel(
    val filters: GitLogFilters = GitLogFilters(),
    val selectedHash: String? = null,
    /** Rows of the visible graph on screen; the driver loads subjects around them. */
    val visibleRows: IntRange = IntRange.EMPTY,
    /** Commit subjects by hash, loaded in pages around [visibleRows] and capped by the driver. */
    val subjects: PersistentMap<String, String> = persistentMapOf(),
    /** Details of [selectedHash]; null while loading. */
    val details: GitCommitDetails? = null,
    /** Branches that contain [selectedHash]; null while loading. */
    val containingBranches: PersistentList<String>? = null,
    /** File of [details] shown in the diff preview. */
    val selectedChange: GitCommitChange? = null,
    val diff: DiffContent? = null,
)

/** Modal Git dialogs; at most one is open ([teksturepako.pakkuDesktop.app.ui.model.ModpackModel.gitDialog]). */
sealed interface GitDialog {
    data object Commit : GitDialog
    /** Update Project (pull). */
    data object Update : GitDialog
    data object Push : GitDialog
    data object Rollback : GitDialog
    /** [startRevision] (a commit hash from the Log) wins over [startPoint]. */
    data class NewBranch(val startPoint: GitBranch? = null, val startRevision: String? = null) : GitDialog
    data class DeleteBranch(val branch: GitBranch) : GitDialog
    data object Stash : GitDialog
    data object Unstash : GitDialog
    data class Reset(val hash: String) : GitDialog
    data class NewTag(val hash: String) : GitDialog
    data class DeleteTag(val name: String) : GitDialog
    /** Create a repository in the open modpack. */
    data object Init : GitDialog
}

/** One native git operation; set as `pendingGitOp` and run by the git driver. */
sealed interface GitOp {
    /** Footer / title-bar status while running. */
    val status: String

    data object Refresh : GitOp {
        override val status get() = "Refreshing…"
    }

    /** `git init -b [branch]` in the open modpack folder. */
    data class Init(val branch: String) : GitOp {
        override val status get() = "Initializing repository…"
    }

    data object Fetch : GitOp {
        override val status get() = "Fetching…"
    }

    data class Pull(val rebase: Boolean) : GitOp {
        override val status get() = "Updating…"
    }

    data class Push(val force: Boolean = false, val tags: Boolean = false) : GitOp {
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

    /** Detached checkout of [hash]. */
    data class CheckoutRevision(val hash: String) : GitOp {
        override val status get() = "Checking out ${hash.take(8)}…"
    }

    /** [merge] picks against the first parent (`-m 1`). */
    data class CherryPick(val hash: String, val merge: Boolean) : GitOp {
        override val status get() = "Cherry-picking ${hash.take(8)}…"
    }

    data class Revert(val hash: String, val merge: Boolean) : GitOp {
        override val status get() = "Reverting ${hash.take(8)}…"
    }

    data class Reset(val hash: String, val mode: GitResetMode) : GitOp {
        override val status get() = "Resetting to ${hash.take(8)}…"
    }

    data class CreateTag(val name: String, val hash: String) : GitOp {
        override val status get() = "Creating tag $name…"
    }

    data class DeleteTag(val name: String) : GitOp {
        override val status get() = "Deleting tag $name…"
    }
}
