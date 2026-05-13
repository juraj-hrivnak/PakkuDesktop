/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git.wrapper

import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitCommit
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

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
    val gitFiles: List<GitFile> = emptyList(),
    val selectedFiles: Set<GitFile> = emptySet(),
    /** Posix paths of folders expanded in the commit changelist tree (UI only). */
    val expandedFolderPaths: Set<String> = emptySet(),
    val branches: Set<GitBranch> = emptySet(),
    val outgoingCommits: Set<GitCommit> = emptySet(),
    val commitMessage: String = "",
)
