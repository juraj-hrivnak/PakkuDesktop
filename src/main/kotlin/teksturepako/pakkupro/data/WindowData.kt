package teksturepako.pakkupro.data

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import teksturepako.pakku.api.data.jsonEncodeDefaults
import java.io.File

@Serializable
data class WindowData(
    val placement: WindowPlacement = WindowPlacement.Floating,
    val x: Float? = WindowPosition.Aligned(Alignment.Center).x.value.takeUnless { it.isNaN() },
    val y: Float? = WindowPosition.Aligned(Alignment.Center).y.value.takeUnless { it.isNaN() },
    val width: Float = 900.0F,
    val height: Float = 700.0F
)
{
    companion object
    {
        private val _json = Json(jsonEncodeDefaults) {
            allowSpecialFloatingPointValues = true
        }

        const val FILE_NAME = "window-data.json"

        fun readOrNew(): WindowData = runCatching {
            _json.decodeFromString<WindowData>(File(FILE_NAME).readText())
        }.getOrNull() ?: WindowData()
    }

    fun write()
    {
        val text = _json.encodeToString(Json.serializersModule.serializer(), this)
        File(FILE_NAME).writeText(text)
    }
}

