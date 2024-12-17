package pakkupro.data

import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

@Serializable
data class WindowData(
    private val isMinimized: Boolean,
    private val width: Double,
    private val height: Double
)
{
    fun toState() = WindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition(Alignment.Center),
        isMinimized = isMinimized,
        width = width.dp,
        height = height.dp
    )

    companion object
    {
        const val FILE_NAME = "window-data.json"

        fun readOrNull(): WindowData? = runCatching {
            Json.decodeFromString<WindowData>(File(FILE_NAME).readText())
        }.getOrNull()

        fun from(window: ComposeWindow) = WindowData(
            isMinimized = window.isMinimized,
            width = window.size.getWidth(),
            height = window.size.getHeight()
        )
    }

    fun write()
    {
        val text = Json.encodeToString(Json.serializersModule.serializer(), this)
        File(FILE_NAME).writeText(text)
    }
}
