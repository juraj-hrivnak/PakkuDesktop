/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import teksturepako.pakkuDesktop.app.git.GitBranch
import teksturepako.pakkuDesktop.app.git.GitCommit
import teksturepako.pakkuDesktop.app.git.GitFile

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
    /** Posix paths of folders expanded in the commit changelist tree (UI only). */
    val expandedFolderPaths: PersistentSet<String> = persistentSetOf(),
    val branches: Set<GitBranch> = emptySet(),
    val outgoingCommits: Set<GitCommit> = emptySet(),
    val commitMessage: String = "",
)
