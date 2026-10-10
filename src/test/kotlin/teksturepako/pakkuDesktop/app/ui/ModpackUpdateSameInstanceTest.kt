/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.datetime.LocalDateTime
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.model.FilesFabAction
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.GitFabAction
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectFileChange
import teksturepako.pakkuDesktop.app.ui.model.ProjectUpdateInfo
import teksturepako.pakkuDesktop.app.ui.model.ProjectsFabAction
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ModpackUpdateSameInstanceTest {
    private val file = GitFile(
        path = "a/b.txt",
        status = GitChange.Modified("a/b.txt"),
        lastModified = LocalDateTime(2026, 1, 1, 0, 0),
        size = 1L,
    )

    private fun assertNoOp(model: ModpackModel, msg: ModpackMsg) =
        assertSame(model, modpackUpdate(msg, model), msg.toString())

    @Test
    fun toastDismissedUnknownId() = assertNoOp(ModpackModel(), ModpackMsg.ToastDismissed("missing"))

    @Test
    fun writeCompletedWhenAlreadyCleared() {
        assertNoOp(ModpackModel(), ModpackMsg.PropertyWriteCompleted)
        assertNoOp(ModpackModel(), ModpackMsg.MetaWriteCompleted)
    }

    @Test
    fun mutationCompletedWhenAlreadyCleared() {
        assertNoOp(ModpackModel(), ModpackMsg.MutationCompleted)
        val pending = ModpackModel(pendingRemovalKeys = persistentSetOf("a"))
        assertNotSame(pending, modpackUpdate(ModpackMsg.MutationCompleted, pending))
    }

    @Test
    fun actionFinishedTwice() {
        val running = modpackUpdate(ModpackMsg.ActionStarted("Fetching"), ModpackModel())
        val finished = modpackUpdate(ModpackMsg.ActionFinished, running)
        assertNotSame(running, finished)
        assertNoOp(finished, ModpackMsg.ActionFinished)
    }

    @Test
    fun fetchProgressUnchanged() =
        assertNoOp(ModpackModel(fetchProgressCompleted = 3, fetchProgressTotal = 10), ModpackMsg.FetchProgressUpdated(3, 10))

    @Test
    fun gitStateUpdatedWithEqualState() {
        val first = modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))), ModpackModel())
        assertNoOp(first, ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file.copy()))))
        assertNotSame(first, modpackUpdate(ModpackMsg.GitStateUpdated(GitState()), first))
    }

    @Test
    fun gitBulkTogglesAlreadyApplied() {
        val model = modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))), ModpackModel())
        assertNoOp(model, ModpackMsg.GitChangelistExpandAllFolders)
        assertNoOp(ModpackModel(), ModpackMsg.GitChangelistCollapseAllFolders)
        assertNoOp(ModpackModel(), ModpackMsg.GitClearChangedFileSelection)
        val allSelected = modpackUpdate(ModpackMsg.GitSelectAllChangedFiles, model)
        assertNoOp(allSelected, ModpackMsg.GitSelectAllChangedFiles)
    }

    @Test
    fun gitDiffAndProgressUnchanged() {
        assertNoOp(ModpackModel(gitDiffPendingFile = file), ModpackMsg.GitDiffFileSelected(file))
        assertNoOp(ModpackModel(), ModpackMsg.GitDiffComputed(null))
        assertNoOp(ModpackModel(), ModpackMsg.GitEventProgressUpdated(null))
    }

    @Test
    fun hiddenDialogsHidden() {
        assertNoOp(ModpackModel(), ModpackMsg.HideAddDialog)
        assertNoOp(ModpackModel(), ModpackMsg.HideRemoveDialog)
        assertNoOp(ModpackModel(), ModpackMsg.HideGitCommitDialog)
        assertNoOp(ModpackModel(), ModpackMsg.HideGitPullDialog)
        assertNoOp(ModpackModel(), ModpackMsg.GitDropdown(GitDropdownMsg.HidePushDialog))
    }

    @Test
    fun addResolveProgressUnchanged() {
        val model = modpackUpdate(ModpackMsg.AddResolveProgress("Resolving…"), ModpackModel())
        assertNoOp(model, ModpackMsg.AddResolveProgress("Resolving…"))
    }

    @Test
    fun tabAndSelectionUnchanged() {
        assertNoOp(ModpackModel(), ModpackMsg.TabSelected(SelectedTab.PROJECTS))
        assertNoOp(ModpackModel(), ModpackMsg.GitDropdown(GitDropdownMsg.TabSelected(SelectedTab.PROJECTS)))
        assertNoOp(ModpackModel(), ModpackMsg.ProjectSelected(null))
        assertNoOp(ModpackModel(), ModpackMsg.ProjectEditing(false))
        assertNoOp(ModpackModel(), ModpackMsg.ModpackEditing(false))
        assertNoOp(ModpackModel(), ModpackMsg.ProjectsCleared())
        val selected = ModpackModel(selectedProjectKeys = persistentSetOf("a", "b"))
        assertNoOp(selected, ModpackMsg.ProjectsSelected(setOf("a")))
        assertNoOp(selected, ModpackMsg.ProjectsDeselected(setOf("c")))
    }

    @Test
    fun rememberedFabUnchanged() {
        val model = ModpackModel(
            lastProjectsFab = ProjectsFabAction.Add,
            lastFilesFab = FilesFabAction.Sync,
            lastGitFab = GitFabAction.Pull,
        )
        assertNoOp(model, ModpackMsg.ProjectsFabRemembered(ProjectsFabAction.Add))
        assertNoOp(model, ModpackMsg.FilesFabRemembered(FilesFabAction.Sync))
        assertNoOp(model, ModpackMsg.GitFabRemembered(GitFabAction.Pull))
    }

    @Test
    fun filtersUnchanged() {
        val model = ModpackModel()
        assertNoOp(model, ModpackMsg.FilterUpdatesOnlyChanged(false))
        assertNoOp(model, ModpackMsg.FilterTypesChanged(emptySet()))
        assertNoOp(model, ModpackMsg.FilterSidesChanged(emptySet()))
        assertNoOp(model, ModpackMsg.FilterMissingSideChanged(false))
        assertNoOp(model, ModpackMsg.FilterProvidersChanged(emptySet()))
        assertNoOp(model, ModpackMsg.FilterRedistributableChanged(null))
        assertNoOp(model, ModpackMsg.FilesFilterFoldersChanged(emptySet()))
        assertNoOp(model, ModpackMsg.FilesFilterChangeKindsChanged(emptySet()))
    }

    @Test
    fun focusUnchanged() {
        assertNoOp(ModpackModel(), ModpackMsg.FilesFilterFocusChanged(false))
        assertNoOp(ModpackModel(), ModpackMsg.ProjectsFilterFocusChanged(false))
        assertNoOp(ModpackModel(), ModpackMsg.FocusProjectsFilterConsumed)
        assertNoOp(ModpackModel(wantsFocusProjectsFilter = true), ModpackMsg.FocusProjectsFilterRequested)
    }

    @Test
    fun updateFileAlreadySelected() {
        val projectFile = ProjectFile(type = "modrinth", id = "f1")
        val project = Project(
            type = ProjectType.MOD,
            slug = mutableMapOf(),
            name = mutableMapOf(),
            id = mutableMapOf(),
            files = mutableSetOf(projectFile),
        )
        val change = ProjectFileChange("mr", projectFile, listOf(projectFile), selectedFileId = "f1")
        val model = ModpackModel(updatePreviews = mapOf("k" to ProjectUpdateInfo(project, listOf(change))).toPersistentMap())
        assertNoOp(model, ModpackMsg.UpdateFileSelected("k", "mr", "f1"))
    }
}
