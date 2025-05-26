package teksturepako.pakkuDesktop.pkui.component.toast

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ToastData(
    val id: String = UUID.randomUUID().toString(),
    val duration: Duration = 4000.milliseconds,
    val content: @Composable BoxScope.() -> Unit,
)