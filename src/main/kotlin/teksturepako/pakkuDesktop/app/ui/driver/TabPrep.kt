/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.github.michaelbull.result.get
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.git.GitChangelistUiSnapshot
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesLink
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredAndSortedProjects
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.projectsListPrepKey
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.snapshotList
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel

/**
 * Files-tab list built off the UI thread. [FilesTab] reads this instead of walking
 * projects and the disk snapshot during composition.
 */
class FilesPanePrep(
    val links: List<FilesLink>,
    val tree: PathTreeSnapshot<FilesLink>,
    val selectableKeys: List<String>,
    val linkIndexByPathKey: Map<String, Int>,
)

val LocalFilesPanePrep = compositionLocalOf<FilesPanePrep?> { null }

/** Changelist tree built off the UI thread. [files] is the same list instance as [GitState.gitFiles]. */
class GitChangelistPrep(
    val files: List<GitFile>,
    val grouped: Boolean,
    val snapshot: GitChangelistUiSnapshot,
)

val LocalGitChangelistPrep = compositionLocalOf<GitChangelistPrep?> { null }

class ProjectsListPrep(
    val key: Any,
    val all: List<Project>,
    val filtered: List<Project>,
    val visibleKeys: List<String>,
)

val LocalProjectsListPrep = compositionLocalOf<ProjectsListPrep?> { null }

/**
 * Prepared project list when it matches [model]; otherwise one filter-and-sort on this thread.
 * Tab switches hit the prepared list. A filter edit misses until the driver catches up.
 */
@Composable
fun projectsListData(model: ProjectsTabModel): ProjectsListPrep {
    val key = model.projectsListPrepKey()
    LocalProjectsListPrep.current?.takeIf { it.key == key }?.let { return it }
    val all = model.lockFile?.get()?.getAllProjects().snapshotList()
    val filtered = model.filteredAndSortedProjects(all)
    return ProjectsListPrep(
        key = key,
        all = all,
        filtered = filtered,
        visibleKeys = filtered.map { it.uiKey() },
    )
}
