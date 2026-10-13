/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/** What the Welcome screen shows on a recent modpack card, read from its config and lock file. */
@Immutable
data class ModpackSummary(
    val state: State,
    val name: String? = null,
    val version: String? = null,
    val author: String? = null,
    val mcVersions: PersistentList<String> = persistentListOf(),
    /** loader name (lowercase) to loader version */
    val loaders: PersistentList<Pair<String, String>> = persistentListOf(),
    /** provider display name, e.g. "Modrinth" */
    val target: String? = null,
    val projectCount: Int? = null,
) {
    enum class State {
        Ready,
        /** directory exists but has no `pakku-lock.json` */
        NotInitialized,
        /** lock file exists but could not be parsed */
        Unreadable,
        Missing,
    }
}
