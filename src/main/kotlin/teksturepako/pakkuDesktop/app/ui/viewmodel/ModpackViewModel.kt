/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import com.github.michaelbull.result.get
import io.klogging.Klogger
import io.klogging.logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.jewel.ui.component.SplitLayoutState
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.ModpackUiState
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.SelectedTab
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.SortOrder
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import java.io.File

object ModpackViewModel
{
    private val logger: Klogger = logger(this::class)

    private val _modpackUiState = MutableStateFlow(ModpackUiState())
    val modpackUiState: StateFlow<ModpackUiState> = _modpackUiState.asStateFlow()

    suspend fun loadFromDisk() = coroutineScope {
        ProfileViewModel.loadFromDisk()
        
        val lockFile = LockFile.readToResult()

        _modpackUiState.update { currentState ->
            currentState.copy(
                lockFile = lockFile
            )
        }

        // Update selected project reference when updating lock file
        if (_modpackUiState.value.selectedProject != null && lockFile.isOk)
        {
            val updatedProject = lockFile.get()?.getAllProjects()?.find { project ->
                project isAlmostTheSameAs _modpackUiState.value.selectedProject!!
            }
            selectProject(updatedProject)
        }

        logger.info {
            "LockFile [$workingPath${File.separator}${LockFile.FILE_NAME}] loaded from disk"
        }

        val configFile = ConfigFile.readToResult()

        _modpackUiState.update { currentState ->
            currentState.copy(
                configFile = configFile
            )
        }

        logger.info {
            "ConfigFile [$workingPath${File.separator}${ConfigFile.FILE_NAME}] loaded from disk"
        }
    }

    fun reset()
    {
        _modpackUiState.update {
            ModpackUiState()
        }
        _modpackUiState.value.action.second?.cancel()
        toasts.value = emptyList()
    }

    fun selectTab(updatedTab: SelectedTab)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                selectedTab = updatedTab
            )
        }
    }

    fun selectProject(project: Project?)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                selectedProject = project
            )
        }
    }

    fun editProject(boolean: Boolean)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                editingProject = boolean
            )
        }
    }

    suspend fun writeEditingProjectToDisk(
        builder: ConfigFile.ProjectConfig.(slug: String) -> Unit
    )
    {
        val lockFile = _modpackUiState.value.lockFile?.get() ?: return
        val configFile = _modpackUiState.value.configFile?.get() ?: return
        val editingProject = _modpackUiState.value.selectedProject ?: return

        configFile.setProjectConfig(editingProject, lockFile, builder)
        configFile.write()
    }

    object ProjectsSelection
    {
        fun select(pakkuId: String)
        {
            val predicate: (Project) -> Boolean = { project -> project.pakkuId == pakkuId }
            _modpackUiState.update { currentState ->
                currentState.copy(
                    selectedProjectsMap = mapOf(pakkuId to predicate)
                )
            }
        }

        fun select(predicate: (Project) -> Boolean)
        {
            _modpackUiState.update { currentState ->
                val projects = currentState.lockFile?.get()?.getAllProjects() ?: return@update currentState

                val newSelections = projects
                    .filter(predicate)
                    .associate { project ->
                        project.pakkuId!! to { p: Project -> p.pakkuId == project.pakkuId }
                    }

                currentState.copy(
                    selectedProjectsMap = currentState.selectedProjectsMap + newSelections
                )
            }
        }

        fun deselect(predicate: (Project) -> Boolean)
        {
            _modpackUiState.update { currentState ->
                val projects = currentState.lockFile?.get()?.getAllProjects() ?: return@update currentState

                val keysToRemove = projects
                    .filter(predicate)
                    .map { it.pakkuId }
                    .toSet()

                currentState.copy(
                    selectedProjectsMap = currentState.selectedProjectsMap.filterKeys { it !in keysToRemove }
                )
            }
        }

        fun toggle(project: Project)
        {
            _modpackUiState.update { currentState ->
                if (isSelected(project)) {
                    currentState.copy(
                        selectedProjectsMap = currentState.selectedProjectsMap - project.pakkuId!!
                    )
                } else {
                    currentState.copy(
                        selectedProjectsMap = currentState.selectedProjectsMap +
                                (project.pakkuId!! to { p: Project -> p.pakkuId == project.pakkuId })
                    )
                }
            }
        }

        fun select(projects: List<Project>)
        {
            _modpackUiState.update { currentState ->
                val newSelections = projects.associate { project ->
                    project.pakkuId!! to { p: Project -> p.pakkuId == project.pakkuId }
                }

                currentState.copy(
                    selectedProjectsMap = currentState.selectedProjectsMap + newSelections
                )
            }
        }

        fun selectRange(startProject: Project, endProject: Project)
        {
            _modpackUiState.update { currentState ->
                val projects = currentState.lockFile?.get()?.getAllProjects() ?: return@update currentState

                val startIndex = projects.indexOf(startProject)
                val endIndex = projects.indexOf(endProject)

                if (startIndex == -1 || endIndex == -1) return@update currentState

                val start = minOf(startIndex, endIndex)
                val end = maxOf(startIndex, endIndex)

                val projectsInRange = projects.slice(start..end)
                val newSelections = projectsInRange.associate { project ->
                    project.pakkuId!! to { p: Project -> p.pakkuId == project.pakkuId }
                }

                currentState.copy(
                    selectedProjectsMap = currentState.selectedProjectsMap + newSelections
                )
            }
        }

        fun clear()
        {
            _modpackUiState.update { currentState ->
                currentState.copy(
                    selectedProjectsMap = emptyMap()
                )
            }
        }

        fun isSelected(project: Project): Boolean =
            _modpackUiState.value.selectedProjectsMap.containsKey(project.pakkuId)
    }

    // -- SORT ORDER --

    fun updateSortOrder(updatedSortOrder: SortOrder)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                sortOrder = updatedSortOrder
            )
        }
    }

    // -- PROJECTS FILTER --

    fun updateProjectsFilter(updatedFilter: (Project) -> Boolean)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                projectsFilter = updatedFilter
            )
        }
    }

    // -- ACTIONS --

    fun runActionWithJob(updatedAction: String, job: Job)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                action = updatedAction to job
            )
        }
    }

    suspend fun terminateAction() = coroutineScope {

        if (_modpackUiState.value.action.second != null)
        {
            try
            {
                _modpackUiState.value.action.second?.cancelAndJoin()
            } catch (_: Exception)
            {
            }

            logger.info { "action job '${_modpackUiState.value.action.first}' cancelled" }
        }
        else
        {
            logger.info { "action job was not found" }
        }

        _modpackUiState.update { currentState ->
            currentState.copy(
                action = null to null
            )
        }
    }

    // -- TOASTS --

    val toasts = mutableStateOf(listOf<ToastData>())

    // -- PROJECTS SCROLL STATE --

    val projectsScrollState = mutableStateOf(LazyListState(0, 0))

    // -- SPLIT LAYOUT STATE --

    val actionSplitState: SplitLayoutState =  SplitLayoutState(1f)
    val projectsTabSplitState: SplitLayoutState =  SplitLayoutState(0.2F)
}
