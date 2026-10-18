package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    fun layoutWidthMatchesScaledDensity() {
        assertEquals(1200.dp, titleBarLayoutWidth(1200.dp, systemDensity = 1.5f, layoutDensity = 1.5f))
        assertEquals(800.dp, titleBarLayoutWidth(1200.dp, systemDensity = 1f, layoutDensity = 1.5f))
    }

    @Test
    fun labelMaxWidthFollowsMeasuredSlot() {
        // Before the first layout pass the slot is unmeasured; labels fall back.
        assertEquals(200.dp, titleBarLabelMaxWidth(Dp.Unspecified))
        // Fraction of the measured slot.
        assertEquals(160.dp, titleBarLabelMaxWidth(400.dp, fraction = 0.4f))
        // But never less than the slot itself on very narrow windows.
        assertEquals(40.dp, titleBarLabelMaxWidth(40.dp, fraction = 0.4f))
    }
}
