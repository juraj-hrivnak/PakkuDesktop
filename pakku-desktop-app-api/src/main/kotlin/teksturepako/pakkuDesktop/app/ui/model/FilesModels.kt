/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/** Disk state for the Files tab. Paths are relative to the working dir with '/' separators. */
@Immutable
data class DiskSnapshot(
    /** pathKey → display path, for every path Pakku expects (lock file clusters + override destinations). */
    val expected: PersistentMap<String, String> = persistentMapOf(),
    /** pathKeys of [expected] that exist. */
    val present: PersistentSet<String> = persistentSetOf(),
    /** Display paths of managed files on disk that are not in [expected]. */
    val untracked: PersistentList<String> = persistentListOf(),
    /** Managed folders that exist, with trailing '/'. */
    val existingFolders: PersistentSet<String> = persistentSetOf(),
)

fun pathKey(relative: String): String = relative.replace('\\', '/').removePrefix("./").lowercase()

/** True when [relative] is an expected file that exists, or an untracked managed file. */
fun DiskSnapshot.isOnDisk(relative: String): Boolean {
    val key = pathKey(relative)
    if (key in present) return true
    return untracked.any { pathKey(it) == key }
}
