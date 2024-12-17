package teksturepako.pakkupro.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.jewel.window.DecoratedWindowScope
import teksturepako.pakkupro.data.WindowData

object WindowViewModel
{
    private val _windowData = MutableStateFlow(WindowData())
    val windowData: StateFlow<WindowData> = _windowData.asStateFlow()

    init
    {
        loadFromDisk()
        writeToDisk()
    }

    fun loadFromDisk()
    {
        val windowDataState = WindowData.readOrNew()

        println("WindowViewModel loaded from disk")

        _windowData.update {
            windowDataState
        }
    }

    fun writeToDisk()
    {
        println("WindowViewModel written to disk")

        // Write to disk
        _windowData.value.write()
    }

    fun updateWindowData(windowState: WindowState)
    {
        loadFromDisk()

        _windowData.update { currentState ->
            currentState.copy(
                placement = windowState.placement,
                x = windowState.position.x.value.takeUnless { it.isNaN() },
                y = windowState.position.y.value.takeUnless { it.isNaN() },
                width = windowState.size.width.value,
                height = windowState.size.height.value,
            )
        }

        writeToDisk()
    }

    // -- WINDOW SCOPE --

    private var _windowScope by mutableStateOf<DecoratedWindowScope?>(null)
    private var _windowScopeInitialized by mutableStateOf(false)

    fun updateWindowScope(windowScope: DecoratedWindowScope)
    {
        _windowScope = windowScope
    }

    fun applyInitialWindowPlacement(coroutineScope: CoroutineScope)
    {
        if (_windowScopeInitialized) return

        coroutineScope.launch {
            _windowScope?.window?.placement = _windowData.value.placement

            delay(10)

            _windowScopeInitialized = true
        }
    }
}