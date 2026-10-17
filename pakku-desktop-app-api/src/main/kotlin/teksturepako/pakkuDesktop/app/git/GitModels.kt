/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface GitChange
{
    val path: String
    val displayName: String get() = path.substringAfterLast('/')

    data class Untracked(override val path: String) : GitChange
    data class Modified(override val path: String) : GitChange
    data class Added(override val path: String) : GitChange
    data class Deleted(override val path: String) : GitChange
}

data class GitBranch(
    val name: String,
    val isRemote: Boolean,
    val isCurrent: Boolean,
)

data class GitCommit(
    val hash: String,
    val message: String,
)

enum class GitRefType {
    LocalBranch,
    RemoteBranch,
    Tag,
}

/** A branch or tag from `git for-each-ref`; [hash] is the commit it points at (peeled for tags). */
data class GitRef(
    val name: String,
    val type: GitRefType,
    val hash: String,
)

/** One file of a commit, from `git diff --name-status` against the first parent. */
data class GitCommitChange(
    val path: String,
    val status: GitChange,
    /** Source path of a rename or copy. */
    val oldPath: String? = null,
)

/** The commit selected in the Log: full message and changed files. */
data class GitCommitDetails(
    val hash: String,
    val parents: List<String>,
    val authorName: String,
    val authorEmail: String,
    val authorTime: Instant,
    val committerName: String,
    val committerEmail: String,
    val commitTime: Instant,
    val message: String,
    val changes: List<GitCommitChange>,
)

/** Author-time range of the Log's Date filter; a null end is open. */
data class GitLogDateFilter(
    val since: Instant?,
    val until: Instant?,
)

/** Log filters, like IntelliJ's filter bar. */
data class GitLogFilters(
    val text: String = "",
    val matchCase: Boolean = false,
    val regex: Boolean = false,
    /** Ref names whose history is shown; null shows every branch. */
    val branches: PersistentSet<String>? = null,
    /** Author names; empty matches everyone. */
    val users: PersistentSet<String> = persistentSetOf(),
    val date: GitLogDateFilter? = null,
)

enum class GitResetMode(val flag: String) {
    Soft("--soft"),
    Mixed("--mixed"),
    Hard("--hard"),
    Keep("--keep"),
}

/** Entry of `git stash list`; [ref] is `stash@{n}`. */
data class GitStash(
    val ref: String,
    val message: String,
)

data class GitFile(
    val path: String,
    val status: GitChange,
    val lastModified: LocalDateTime,
    val size: Long,
    val isSelected: Boolean = false
)
{
    companion object
    {
        fun fromFileInfo(
            path: String,
            status: GitChange,
            modifiedEpochMillis: Long,
            size: Long,
            isSelected: Boolean = false
        ): GitFile = GitFile(
            path = path,
            status = status,
            lastModified = Instant
                .fromEpochMilliseconds(modifiedEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            size = size,
            isSelected = isSelected
        )
    }
}

data class DiffContent(
    val oldPath: String?,
    val newPath: String,
    val hunks: List<DiffHunk>
)

data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>
)

data class DiffLine(
    val number: LineNumbers,
    val content: String,
    val type: DiffType
)
{
    data class LineNumbers(
        val old: Int?,
        val new: Int?
    )
}

enum class DiffType
{
    ADDED,
    DELETED,
    UNCHANGED,
}