package teksturepako.pakkupro.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import teksturepako.pakkupro.ui.viewmodel.state.ModpackUiState
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab

object ModpackViewModel
{
    private val _modpackUiState = MutableStateFlow(ModpackUiState())
    val modpackUiState: StateFlow<ModpackUiState> = _modpackUiState.asStateFlow()

    fun selectTab(updatedTab: SelectedTab)
    {
        _modpackUiState.update { currentState ->
            currentState.copy(
                selectedTab = updatedTab
            )
        }
    }
}
