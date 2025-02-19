package teksturepako.pakkupro.ui.viewmodel

import com.github.michaelbull.result.get
import io.klogging.Klogger
import io.klogging.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkupro.ui.viewmodel.state.ModpackUiState
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab
import java.io.File

object ModpackViewModel
{
    private val logger: Klogger = logger(this::class)

    private val _modpackUiState = MutableStateFlow(ModpackUiState())
    val modpackUiState: StateFlow<ModpackUiState> = _modpackUiState.asStateFlow()

    suspend fun loadFromDisk() = coroutineScope {
        launch {
            ProfileViewModel.profileData.value.currentProfile?.path?.let {
                workingPath = it
                logger.info { "workingPath set to [$workingPath]" }
            }
        }.join()

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

    fun updateFilter(updatedFilter: (Project) -> Boolean)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                projectsFilter = updatedFilter
            )
        }
    }

    fun runActionWithJob(updatedAction: String, job: Job)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                action = updatedAction to job
            )
        }
    }

    suspend fun terminateAction()
    {
        if (_modpackUiState.value.action.second != null)
        {
            try
            {
                _modpackUiState.value.action.second?.cancelAndJoin()
            }
            catch (_: CancellationException)
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

    // -- TOASTER --
}
