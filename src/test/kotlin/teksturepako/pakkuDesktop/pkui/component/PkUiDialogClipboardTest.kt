/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.Test
import kotlin.test.assertSame
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests

class PkUiDialogClipboardTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private object MarkerClipboard : Clipboard {
        override suspend fun getClipEntry(): ClipEntry? = null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
        override val nativeClipboard: Any get() = this
    }

    @Test
    fun `dialog content sees the window clipboard`() {
        var inside: Clipboard? = null
        composeRule.setContent {
            val textStyle = remember { JewelTheme.createDefaultTextStyle() }
            val theme = remember { JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle) }
            IntUiTheme(theme, ComponentStyling, swingCompatMode = false) {
                CompositionLocalProvider(LocalClipboard provides MarkerClipboard) {
                    PkUiDialog(onDismiss = {}, title = "Dialog") {
                        inside = LocalClipboard.current
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertSame(MarkerClipboard, inside)
    }
}
