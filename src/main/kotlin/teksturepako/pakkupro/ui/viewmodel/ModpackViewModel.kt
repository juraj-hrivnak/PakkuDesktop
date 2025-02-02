package teksturepako.pakkupro.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dokar.sonner.ToasterState
import com.github.michaelbull.result.get
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkupro.ui.viewmodel.state.ModpackUiState
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab

object ModpackViewModel
{
    private val _modpackUiState = MutableStateFlow(ModpackUiState())
    val modpackUiState: StateFlow<ModpackUiState> = _modpackUiState.asStateFlow()

    suspend fun loadFromDisk()
    {
        val lockFile = LockFile.readToResult().getOrNull() ?: return

        _modpackUiState.update { currentState ->
            currentState.copy(
                lockFile = lockFile
            )
        }

        if (_modpackUiState.value.selectedProject != null)
        {
            val updatedProject = lockFile.getAllProjects().find { project ->
                project isAlmostTheSameAs _modpackUiState.value.selectedProject!!
            }
            selectProject(updatedProject)
        }

        println("ModpackViewModel (LockFile) loaded from disk")

        val configFile = ConfigFile.readToResult().get() ?: return

        _modpackUiState.update { currentState ->
            currentState.copy(
                configFile = configFile
            )
        }

        println("ModpackViewModel (ConfigFile) loaded from disk")
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
        val lockFile = _modpackUiState.value.lockFile ?: return
        val configFile = _modpackUiState.value.configFile ?: return
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

    fun updateAction(updatedAction: String?, job: Job? = null)
    {
        if (updatedAction != null)
        {
            _modpackUiState.update { currentState ->
                currentState.copy(
                    action = updatedAction to job
                )
            }
            println("ModpackViewModel action updated to '$updatedAction'")
        }
        else
        {
            job?.cancel()
            _modpackUiState.update { currentState ->
                currentState.copy(
                    action = null to null
                )
            }
        }
    }

    // -- TOASTER --

    var toasterState: ToasterState? by mutableStateOf(null)
}
