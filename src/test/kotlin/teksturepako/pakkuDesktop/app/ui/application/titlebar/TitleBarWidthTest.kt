package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.ui.unit.dp
import dev.nucleusframework.window.utils.linux.LinuxButtonLayout
import dev.nucleusframework.window.utils.linux.LinuxTitleBarButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitleBarWidthTest {
    @Test
    fun tiersFollowWindowWidth() {
        assertEquals(TitleBarWidthTier.Comfortable, titleBarWidthTier(900.dp))
        assertEquals(TitleBarWidthTier.Compact, titleBarWidthTier(879.dp))
    }

    @Test
    fun startClusterNeverExceedsWindow() {
        val layout = LinuxButtonLayout(
            buttons = listOf(
                LinuxTitleBarButton.CLOSE,
                LinuxTitleBarButton.MINIMIZE,
                LinuxTitleBarButton.MAXIMIZE,
            ),
            controlsOnRight = true,
        )
        val narrow = titleBarStartClusterMaxWidth(320.dp, layout)
        assertTrue(narrow <= 320.dp)
        assertTrue(titleBarStartClusterMaxWidth(900.dp, layout) > titleBarStartClusterMaxWidth(500.dp, layout))
    }
}
