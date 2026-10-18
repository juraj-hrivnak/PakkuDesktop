/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import androidx.compose.runtime.Immutable
import com.github.michaelbull.result.Result
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.git.GitState
import teksturepako.pakkuDesktop.app.git.DiffContent
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.git.GitLogModel
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.git.GitRef

/*
 * Per-tab slices of [ModpackModel]. Property names mirror [ModpackModel] so tab bodies read
 * `model.x` unchanged.
 *
 * Slices are projected in the view on every [ModpackModel] change. They are `@Immutable`, so
 * Compose compares them with `equals`, which is per-field identity in practice (reducers keep
 * unchanged fields as the same instances). A tab therefore skips unless a field it reads changed.
 * Only add fields the tab subtree actually reads.
 */

@Immutable
data class ProjectsTabModel(
    val lockFile: Result<LockFile, ActionError>?,
    val configFile: Result<ConfigFile, ActionError>?,
    val selectedTab: SelectedTab,
    val selectedProject: Project?,
    val editingProject: Boolean,
    val selectedProjectKeys: PersistentSet<String>,
    val lastProjectsFab: ProjectsFabAction?,
    val sortOrder: SortOrder,
    val projectsFilterText: String,
    val filterTypes: PersistentSet<ProjectType>,
    val filterSides: PersistentSet<ProjectSide>,
    val filterMissingSide: Boolean,
    val filterProviders: PersistentSet<String>,
    val filterRedistributable: Boolean?,
    val projectsSplitRatio: Float,
    val filterUpdatesOnly: Boolean,
    val updatePreviews: PersistentMap<String, ProjectUpdateInfo>?,
    val actionName: String?,
    val wantsFocusProjectsFilter: Boolean,
    val filesPreviewMode: FilesPreviewMode,
    val syncAdded: PersistentList<Project>,
    val syncRemoved: PersistentList<Project>,
    val syncUpdated: PersistentList<Project>,
)

fun ModpackModel.projectsTab() = ProjectsTabModel(
    lockFile = lockFile,
    configFile = configFile,
    selectedTab = selectedTab,
    selectedProject = selectedProject,
    editingProject = editingProject,
    selectedProjectKeys = selectedProjectKeys,
    lastProjectsFab = lastProjectsFab,
    sortOrder = sortOrder,
    projectsFilterText = projectsFilterText,
    filterTypes = filterTypes,
    filterSides = filterSides,
    filterMissingSide = filterMissingSide,
    filterProviders = filterProviders,
    filterRedistributable = filterRedistributable,
    projectsSplitRatio = projectsSplitRatio,
    filterUpdatesOnly = filterUpdatesOnly,
    updatePreviews = updatePreviews,
    actionName = actionName,
    wantsFocusProjectsFilter = wantsFocusProjectsFilter,
    filesPreviewMode = filesPreviewMode,
    syncAdded = syncAdded,
    syncRemoved = syncRemoved,
    syncUpdated = syncUpdated,
)

@Immutable
data class FilesTabModel(
    val lockFile: Result<LockFile, ActionError>?,
    val configFile: Result<ConfigFile, ActionError>?,
    val selectedProject: Project?,
    val selectedProjectKeys: PersistentSet<String>,
    val lastFilesFab: FilesFabAction?,
    val filesSplitRatio: Float,
    val actionName: String?,
    val filesPreviewMode: FilesPreviewMode,
    val fetchDownloads: PersistentList<FetchDownloadPreview>,
    val fetchOverrides: PersistentList<FetchOverridePreview>,
    val manualOverrides: PersistentList<FetchOverridePreview>,
    val diskSnapshot: DiskSnapshot,
    val fetchCleanup: PersistentList<FetchCleanupPreview>,
    val filesHashMismatchKeys: PersistentSet<String>,
    val syncAdded: PersistentList<Project>,
    val syncRemoved: PersistentList<Project>,
    val syncUpdated: PersistentList<Project>,
    val filesFilterText: String,
    val filesFilterFolders: PersistentSet<String>,
    val filesFilterChangeKinds: PersistentSet<FilesChangeKind>,
    val wantsFocusProjectsFilter: Boolean,
    val filesCollapsedFolders: PersistentSet<String>,
)

fun ModpackModel.filesTab() = FilesTabModel(
    lockFile = lockFile,
    configFile = configFile,
    selectedProject = selectedProject,
    selectedProjectKeys = selectedProjectKeys,
    lastFilesFab = lastFilesFab,
    filesSplitRatio = filesSplitRatio,
    actionName = actionName,
    filesPreviewMode = filesPreviewMode,
    fetchDownloads = fetchDownloads,
    fetchOverrides = fetchOverrides,
    manualOverrides = manualOverrides,
    diskSnapshot = diskSnapshot,
    fetchCleanup = fetchCleanup,
    filesHashMismatchKeys = filesHashMismatchKeys,
    syncAdded = syncAdded,
    syncRemoved = syncRemoved,
    syncUpdated = syncUpdated,
    filesFilterText = filesFilterText,
    filesFilterFolders = filesFilterFolders,
    filesFilterChangeKinds = filesFilterChangeKinds,
    wantsFocusProjectsFilter = wantsFocusProjectsFilter,
    filesCollapsedFolders = filesCollapsedFolders,
)

@Immutable
data class ModpackTabModel(
    val lockFile: Result<LockFile, ActionError>?,
    val configFile: Result<ConfigFile, ActionError>?,
    val metaErrors: PersistentMap<String, PersistentList<ActionError>>,
    val overviewMode: OverviewMode,
    val overviewShowDiff: Boolean,
    val configText: ConfigFileText?,
    val configDiff: DiffContent?,
)

fun ModpackModel.modpackTab() = ModpackTabModel(
    lockFile = lockFile,
    configFile = configFile,
    metaErrors = modpackMetaErrors,
    overviewMode = overviewMode,
    overviewShowDiff = overviewShowDiff,
    configText = configText,
    configDiff = configDiff,
)

fun ModpackTabModel.metaFieldErrors(field: String): List<ActionError> =
    metaErrors[field]?.orEmpty() ?: emptyList()

@Immutable
data class GitTabModel(
    val git: GitState,
    val gitCurrentDiff: DiffContent?,
    val gitDiffPendingFile: GitFile?,
    val pendingGitOp: GitOp?,
    /** [ModpackModel.gitBusy] */
    val gitBusy: Boolean,
    val lastGitFab: GitFabAction?,
    val groupUnversioned: Boolean,
    val gitFilterText: String,
)

fun ModpackModel.gitTab() = GitTabModel(
    git = git,
    gitCurrentDiff = gitCurrentDiff,
    gitDiffPendingFile = gitDiffPendingFile,
    pendingGitOp = pendingGitOp,
    gitBusy = gitBusy(),
    lastGitFab = lastGitFab,
    groupUnversioned = gitGroupUnversioned,
    gitFilterText = gitFilterText,
)

@Immutable
data class LogTabModel(
    val refs: PersistentList<GitRef>,
    val headHash: String?,
    val currentBranchName: String?,
    val gitBusy: Boolean,
    val gitLog: GitLogModel,
)

fun ModpackModel.logTab() = LogTabModel(
    refs = git.refs,
    headHash = git.headHash,
    currentBranchName = git.currentBranch?.name,
    gitBusy = gitBusy(),
    gitLog = gitLog,
)
