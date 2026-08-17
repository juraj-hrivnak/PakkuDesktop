/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.actions.AdditionPlan
import teksturepako.pakkuDesktop.app.actions.RemovalPlan
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import teksturepako.pakkuDesktop.pro.git.wrapper.GitEvent
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitBranch
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

/** pending project property write for projectEditDriver */
data class PropertyWrite(
    val write: ConfigFile.ProjectConfig.(slug: String) -> Unit,
)

/** pack metadata mutation; projectEditDriver applies + writes */
data class MetaWrite(
    val mutate: (ConfigFile, LockFile) -> Unit,
)

/** `pakku init` inputs */
data class InitSpec(
    val name: String,
    val mcVersion: String,
    val loader: String,
    val target: String,
)

/** one provider file change from `pakku status` */
data class ProjectFileChange(
    val providerShortName: String,
    val oldFile: ProjectFile,
    /** newest-first */
    val newFiles: List<ProjectFile>,
    /** chosen [ProjectFile.id] (default: newest) */
    val selectedFileId: String,
) {
    val selectedFile: ProjectFile
        get() = newFiles.firstOrNull { it.fileKey() == selectedFileId } ?: newFiles.first()
}

private fun ProjectFile.fileKey(): String = id.ifEmpty { fileName }

/** status-check result for one project (by pakkuId) */
data class ProjectUpdateInfo(
    val updatedProject: Project,
    val fileChanges: List<ProjectFileChange>,
    /** updated from this preview; stays until next status check */
    val applied: Boolean = false,
) {
    /** lock-ready project with one selected file per changed provider */
    fun projectWithSelectedFiles(): Project {
        val selectedByType = fileChanges.associate { it.oldFile.type to it.selectedFile }
        val files = updatedProject.files
            .groupBy { it.type }
            .map { (type, filesForType) ->
                selectedByType[type] ?: filesForType.maxByOrNull { it.datePublished } ?: filesForType.first()
            }
            .toMutableSet()
        return updatedProject.copy(files = files)
    }
}

// -- ModpackDropdown --

data class ModpackDropdownModel(
    val profileData: ProfileData = ProfileData(),
    val actionEnabled: Boolean = true,
    val enabled: Boolean = true,
)

sealed interface ModpackDropdownMsg {
    // parent
    data class CloseRequested(val force: Boolean = false) : ModpackDropdownMsg
    data object Export : ModpackDropdownMsg
    data object Fetch : ModpackDropdownMsg
    data object ShowSettings : ModpackDropdownMsg
    data class DirectoryPicked(val path: String) : ModpackDropdownMsg
}

// -- GitDropdown --

data class GitDropdownModel(
    val gitState: GitState = GitState(),
    val pushDialogVisible: Boolean = false,
)

sealed interface GitDropdownMsg {
    data object ShowPushDialog : GitDropdownMsg
    data object HidePushDialog : GitDropdownMsg
    // parent
    data object PullRequested : GitDropdownMsg
    data object PushRequested : GitDropdownMsg
    data class TabSelected(val tab: SelectedTab) : GitDropdownMsg
    data class CheckoutRequested(val branch: GitBranch) : GitDropdownMsg
}

// -- ModpackMsg --

sealed interface ModpackMsg {

    // -- Driver messages --

    data class Loaded(
        val lockFile: Result<LockFile, ActionError>,
        val configFile: Result<ConfigFile, ActionError>,
        /**
         * false → drop updatePreviews (project set may have changed).
         * default true keeps last status check across soft reloads.
         */
        val retainUpdatePreviews: Boolean = true,
    ) : ModpackMsg

    data object Reset : ModpackMsg

    // -- Cross-cutting (parent) --

    data object ShowSettings : ModpackMsg
    data object ShowNewModpack : ModpackMsg

    /** forceClose skips the close dialog */
    data class CloseRequested(val forceClose: Boolean = false) : ModpackMsg

    data class DirectoryPicked(val path: String) : ModpackMsg

    // -- Tab --

    data class TabSelected(val tab: SelectedTab) : ModpackMsg

    // -- Projects --

    data class ProjectSelected(val project: Project?) : ModpackMsg
    data class ProjectEditing(val editing: Boolean) : ModpackMsg
    /** Modpack tab edit mode */
    data class ModpackEditing(val editing: Boolean) : ModpackMsg

    data class ProjectsSelected(val pakkuIds: Set<String>) : ModpackMsg
    data class ProjectsDeselected(val pakkuIds: Set<String>) : ModpackMsg
    data class ProjectsCleared(val dummy: Unit = Unit) : ModpackMsg

    // -- Sort / filter --

    data class SortOrderChanged(val order: SortOrder) : ModpackMsg
    data class FilterTextChanged(val text: String) : ModpackMsg
    data class FilterUpdatesOnlyChanged(val enabled: Boolean) : ModpackMsg
    data class FilterTypesChanged(val types: Set<ProjectType>) : ModpackMsg
    data class FilterSidesChanged(val sides: Set<ProjectSide>) : ModpackMsg
    data class FilterProvidersChanged(val providers: Set<String>) : ModpackMsg
    data class ProjectsSplitRatioChanged(val ratio: Float) : ModpackMsg

    /** focus filter (Ctrl/Cmd+F) */
    data object FocusProjectsFilterRequested : ModpackMsg
    data object FocusProjectsFilterConsumed : ModpackMsg
    data class ProjectsFilterFocusChanged(val focused: Boolean) : ModpackMsg

    /** Add projects dialog (FAB) */
    data object ShowAddDialog : ModpackMsg
    data object HideAddDialog : ModpackMsg

    /** Remove projects dialog (Delete / FAB) */
    data object ShowRemoveDialog : ModpackMsg
    data object HideRemoveDialog : ModpackMsg

    /** select all filtered (Ctrl/Cmd+A) */
    data object SelectAllFilteredRequested : ModpackMsg

    /** open detail for primary selection (Enter) */
    data object OpenDetailRequested : ModpackMsg

    /** `pakku status` network check (no write) */
    data object StatusCheckRequested : ModpackMsg
    data class StatusCheckCompleted(val previews: Map<String, ProjectUpdateInfo>) : ModpackMsg
    /** mark previews applied after update */
    data class UpdatesApplied(val pakkuIds: Set<String>) : ModpackMsg
    /** pick candidate file on a status preview */
    data class UpdateFileSelected(
        val pakkuId: String,
        val providerShortName: String,
        val fileId: String,
    ) : ModpackMsg

    // -- Actions --

    data object ExportRequested : ModpackMsg
    data object FetchRequested : ModpackMsg
    data class UpdateRequested(val pakkuIds: Set<String>) : ModpackMsg
    /** DnD auto-add; skips non-recommended */
    data class AddRequested(val query: String) : ModpackMsg
    data class AddPlanConfirmed(val plan: AdditionPlan) : ModpackMsg
    /** direct remove by ids (+ recommended orphaned deps) */
    data class RemoveRequested(val pakkuIds: Set<String>) : ModpackMsg
    data class RemovePlanConfirmed(val plan: RemovalPlan) : ModpackMsg
    data class InitRequested(val spec: InitSpec) : ModpackMsg
    data class FilesDropped(val paths: List<String>) : ModpackMsg
    data class ActionStarted(val name: String) : ModpackMsg
    data object ActionFinished : ModpackMsg
    data object TerminateAction : ModpackMsg

    /** projectMutationDriver after remove/add/init */
    data object MutationCompleted : ModpackMsg

    /** dismiss lock error (not FileNotFound) */
    data object DismissLockError : ModpackMsg

    /** dismiss config error (not FileNotFound) */
    data object DismissConfigError : ModpackMsg

    // -- Project property editing --

    /** projectEditDriver writes, then Loaded */
    data class PropertyWriteRequested(val request: PropertyWrite) : ModpackMsg
    data object PropertyWriteCompleted : ModpackMsg

    /** Modpack tab metadata write */
    data class MetaWriteRequested(val request: MetaWrite) : ModpackMsg
    data object MetaWriteCompleted : ModpackMsg

    // -- Toasts --

    data class ToastAdded(val toast: ToastData) : ModpackMsg
    data class ToastDismissed(val id: String) : ModpackMsg

    // -- Git (Pro) --

    data class GitStateUpdated(val state: GitState) : ModpackMsg
    data class GitFileSelectionToggled(val file: GitFile) : ModpackMsg
    /** toggle all files under folderPath */
    data class GitFolderSelectionToggled(val folderPath: String) : ModpackMsg
    data class GitChangelistFolderExpansionToggled(val folderPath: String) : ModpackMsg
    data object GitChangelistExpandAllFolders : ModpackMsg
    data object GitChangelistCollapseAllFolders : ModpackMsg
    data object GitSelectAllChangedFiles : ModpackMsg
    data object GitClearChangedFileSelection : ModpackMsg
    data class GitCommitMessageChanged(val message: String) : ModpackMsg
    data class GitDiffFileSelected(val file: GitFile) : ModpackMsg
    data class GitDiffComputed(val diff: DiffContent?) : ModpackMsg
    data class GitEventProgressUpdated(val progress: GitEvent.Progress?) : ModpackMsg

    data object GitPullRequested : ModpackMsg
    data object GitPullFinished : ModpackMsg
    data object GitPushRequested : ModpackMsg
    data object GitPushFinished : ModpackMsg
    data object GitCommitRequested : ModpackMsg
    /** gitDriver after commit; success=false if cmd failed */
    data class GitCommitFinished(val success: Boolean) : ModpackMsg
    data class GitCheckoutRequested(val branch: GitBranch) : ModpackMsg
    data object GitCheckoutFinished : ModpackMsg

    // -- Dropdown children --

    data class ModpackDropdown(val msg: ModpackDropdownMsg) : ModpackMsg
    data class GitDropdown(val msg: GitDropdownMsg) : ModpackMsg
}
