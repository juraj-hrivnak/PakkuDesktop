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
import teksturepako.pakkuDesktop.app.git.*
import teksturepako.pakkuDesktop.app.ui.model.*
import kotlin.test.*

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
        assertNoOp(
            ModpackModel(),
            ModpackMsg.MetaWriteFailed(fieldKey = "target", errors = emptyList()),
        )
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
    fun gitRefreshKeepsFileListInstanceWhenOnlyBranchChanges() {
        val first = modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))), ModpackModel())
        val switched = modpackUpdate(
            ModpackMsg.GitStateUpdated(
                GitState(
                    gitFiles = persistentListOf(file.copy()),
                    branches = setOf(GitBranch("feature", isRemote = false, isCurrent = true)),
                )
            ),
            first,
        )
        assertNotSame(first, switched)
        // The changelist tree is cached against this list by identity.
        assertSame(first.git.gitFiles, switched.git.gitFiles)
        assertEquals("feature", switched.git.currentBranch?.name)
        assertSame(first.gitDropdown.gitState.gitFiles, switched.gitDropdown.gitState.gitFiles)
    }

    @Test
    fun gitRefreshKeepsTypedCommitMessageAndSelection() {
        val typed = modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))), ModpackModel())
            .let { modpackUpdate(ModpackMsg.GitCommitMessageChanged("wip"), it) }
            .let { modpackUpdate(ModpackMsg.GitSelectAllChangedFiles, it) }
        val other = file.copy(path = "c.txt", status = GitChange.Untracked("c.txt"))
        val refreshed = modpackUpdate(
            ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file.copy(), other))),
            typed,
        )
        assertEquals("wip", refreshed.git.commitMessage)
        assertEquals(setOf(file.path), refreshed.git.selectedFiles.map { it.path }.toSet())
    }

    @Test
    fun gitRefreshFollowsTheOpenDiff() {
        val diff = DiffContent(oldPath = file.path, newPath = file.path, hunks = emptyList())
        val viewing = modpackUpdate(ModpackMsg.GitDiffComputed(diff),
            modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))), ModpackModel())
        )
        assertEquals(diff, viewing.gitCurrentDiff)

        val edited = file.copy(size = 2L)
        val changed = modpackUpdate(ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(edited))), viewing)
        assertEquals(edited, changed.gitDiffPendingFile)

        val committed = modpackUpdate(ModpackMsg.GitStateUpdated(GitState()), viewing)
        assertNull(committed.gitCurrentDiff)
        assertNull(committed.gitDiffPendingFile)
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
        assertNoOp(ModpackModel(), ModpackMsg.HideGitDialog)
    }

    @Test
    fun gitDialogsAndOpsUnchanged() {
        val open = ModpackModel(gitDialog = GitDialog.Push)
        assertNoOp(open, ModpackMsg.ShowGitDialog(GitDialog.Push))
        val busy = modpackUpdate(ModpackMsg.GitOpRequested(GitOp.Fetch), ModpackModel())
        assertNoOp(busy, ModpackMsg.GitOpRequested(GitOp.Refresh))
        assertNoOp(busy, ModpackMsg.ShowGitDialog(GitDialog.Commit))
        assertNoOp(busy, ModpackMsg.GitOpFinished(GitOp.Refresh, success = true))
        assertNoOp(ModpackModel(), ModpackMsg.GitGroupUnversionedChanged(grouped = true))
        assertNoOp(ModpackModel(), ModpackMsg.GitFilterTextChanged(""))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogCommitSelected(null))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogVisibleRowsChanged(IntRange.EMPTY))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogFiltersChanged(GitLogFilters()))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogSubjectsLoaded(emptyMap()))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogChangeSelected(null))
        val other = GitCommitDetails(
            hash = "abc",
            parents = emptyList(),
            authorName = "A",
            authorEmail = "a@b",
            authorTime = kotlinx.datetime.Instant.fromEpochSeconds(0),
            committerName = "A",
            committerEmail = "a@b",
            commitTime = kotlinx.datetime.Instant.fromEpochSeconds(0),
            message = "m",
            changes = emptyList(),
        )
        assertNoOp(ModpackModel(), ModpackMsg.GitLogDetailsLoaded(other))
        assertNoOp(ModpackModel(), ModpackMsg.GitLogContainingBranchesLoaded("abc", emptyList()))
    }

    @Test
    fun gitOpLifecycle() {
        val commit = GitOp.Commit(message = "msg", paths = listOf(file.path))
        val withSelection = modpackUpdate(
            ModpackMsg.GitStateUpdated(GitState(gitFiles = persistentListOf(file))),
            ModpackModel(gitDialog = GitDialog.Commit),
        ).let { modpackUpdate(ModpackMsg.GitCommitMessageChanged("msg"), it) }
            .let { modpackUpdate(ModpackMsg.GitSelectAllChangedFiles, it) }

        val running = modpackUpdate(ModpackMsg.GitOpRequested(commit), withSelection)
        assertEquals(commit, running.pendingGitOp)
        assertNull(running.gitDialog)
        assertTrue(running.gitDropdown.busy)

        val failed = modpackUpdate(ModpackMsg.GitOpFinished(commit, success = false), running)
        assertNull(failed.pendingGitOp)
        assertFalse(failed.gitDropdown.busy)
        assertEquals("msg", failed.git.commitMessage)

        val done = modpackUpdate(ModpackMsg.GitOpFinished(commit, success = true), running)
        assertEquals("", done.git.commitMessage)
        assertTrue(done.git.selectedFiles.isEmpty())
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
    fun filesFolderExpansionNoOps() {
        assertNoOp(ModpackModel(), ModpackMsg.FilesExpandAllFolders)
        val collapsed = modpackUpdate(ModpackMsg.FilesCollapseAllFolders(setOf("mods")), ModpackModel())
        assertNoOp(collapsed, ModpackMsg.FilesCollapseAllFolders(setOf("mods")))
    }

    @Test
    fun focusUnchanged() {
        assertNoOp(ModpackModel(), ModpackMsg.FilesFilterFocusChanged(false))
        assertNoOp(ModpackModel(), ModpackMsg.ProjectsFilterFocusChanged(false))
        assertNoOp(ModpackModel(), ModpackMsg.FocusProjectsFilterConsumed)
        assertNoOp(ModpackModel(wantsFocusProjectsFilter = true), ModpackMsg.FocusProjectsFilterRequested)
    }

    @Test
    fun diskSnapshotLoadedWithEqualSnapshot() {
        assertNoOp(ModpackModel(), ModpackMsg.DiskSnapshotLoaded(DiskSnapshot()))
        val snapshot = DiskSnapshot(
            expected = mapOf(pathKey("mods/a.jar") to "mods/a.jar").toPersistentMap(),
            present = persistentSetOf(pathKey("mods/a.jar")),
            untracked = persistentListOf("mods/sub/b.jar"),
            existingFolders = persistentSetOf("mods/"),
        )
        val empty = ModpackModel()
        val loaded = modpackUpdate(ModpackMsg.DiskSnapshotLoaded(snapshot), empty)
        assertNotSame(empty, loaded)
        val again = DiskSnapshot(
            expected = mapOf(pathKey("mods/a.jar") to "mods/a.jar").toPersistentMap(),
            present = persistentSetOf(pathKey("mods/a.jar")),
            untracked = persistentListOf("mods/sub/b.jar"),
            existingFolders = persistentSetOf("mods/"),
        )
        assertNoOp(loaded, ModpackMsg.DiskSnapshotLoaded(again))
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
