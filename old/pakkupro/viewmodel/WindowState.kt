package pakkupro.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState

sealed class WindowType
{
    @Composable
    abstract fun getState(): WindowState

    data object APP : WindowType()
    {
        @Composable
        override fun getState(): WindowState= rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition(Alignment.Center),
            isMinimized = false,
            width = 900.dp,
            height = 700.dp
        )
    }

    data object LOADING : WindowType()
    {
        @Composable
        override fun getState(): WindowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition(Alignment.Center),
            isMinimized = false,
            width = 300.dp,
            height = 150.dp
        )
    }
}