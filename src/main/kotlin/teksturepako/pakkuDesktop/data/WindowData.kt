package teksturepako.pakkuDesktop.data

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import com.github.michaelbull.result.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.writeToFile
import kotlin.io.path.Path

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

        suspend fun readOrNew(): WindowData =
            decodeToResult<WindowData>(Path(FILE_NAME), format = _json).get() ?: WindowData()
    }

    suspend fun write(): ActionError? = writeToFile<WindowData>(this, FILE_NAME, format = _json)
}

