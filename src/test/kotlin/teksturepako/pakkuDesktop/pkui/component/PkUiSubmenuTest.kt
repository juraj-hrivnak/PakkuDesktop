/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests

class PkUiSubmenuTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private var rootClicks = 0

    private fun setMenu() {
        composeRule.setContent {
            val textStyle = remember { JewelTheme.createDefaultTextStyle() }
            val theme = remember { JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle) }
            IntUiTheme(theme, ComponentStyling, swingCompatMode = false) {
                Box(Modifier.size(800.dp, 600.dp)) {
                    PkUiDropdown(
                        menuModifier = Modifier.width(200.dp),
                        menuContent = {
                            selectableItem(false, onClick = { rootClicks++ }) { Text("Root action") }
                            listOf("A", "B").forEach { name ->
                                pkUiSubmenu(submenu = {
                                    selectableItem(false, onClick = {}) { Text("Checkout $name") }
                                }) { Text("Branch $name") }
                            }
                        },
                    ) { Text("Open") }
                }
            }
        }
        composeRule.onNodeWithText("Open").performClick()
        composeRule.waitForIdle()
    }

    private fun hover(text: String) {
        composeRule.onNodeWithText(text).performMouseInput { moveTo(center) }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()
    }

    @Test
    fun `hovering another branch while a submenu is open switches submenus`() {
        setMenu()

        hover("Branch A")
        composeRule.onNodeWithText("Checkout A").assertExists()

        hover("Branch B")
        composeRule.onNodeWithText("Checkout B").assertExists()
        composeRule.onNodeWithText("Checkout A").assertDoesNotExist()
    }

    @Test
    fun `parent menu items stay clickable while a submenu is open`() {
        setMenu()

        hover("Branch A")
        composeRule.onNodeWithText("Checkout A").assertExists()

        composeRule.onNodeWithText("Root action").performClick()
        composeRule.waitForIdle()
        assertEquals(1, rootClicks)
    }
}
