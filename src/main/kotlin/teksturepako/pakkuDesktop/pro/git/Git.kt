/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

/**
 * Progress events surfaced from JGit [org.eclipse.jgit.lib.ProgressMonitor] into the ELM model.
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
