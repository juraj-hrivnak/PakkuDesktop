/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

private data class TestModel(val pendingJob: String? = null, val noise: Int = 0)

private sealed interface TestMsg {
    data class Unrelated(val n: Int) : TestMsg
    data class StartJob(val id: String) : TestMsg
    data object JobDone : TestMsg
    data class BumpNoise(val n: Int) : TestMsg
}

private fun testUpdate(msg: TestMsg, model: TestModel): TestModel = when (msg) {
    is TestMsg.Unrelated -> model
    is TestMsg.StartJob -> model.copy(pendingJob = msg.id)
    TestMsg.JobDone -> model.copy(pendingJob = null)
    is TestMsg.BumpNoise -> model.copy(noise = msg.n)
}

private val testComponent = component(
    init = TestModel(),
    update = ::testUpdate,
    view = { _, _ -> Box(Modifier) {} },
)

internal fun assumeAwtForComposeUiTests(): TestRule = TestRule { base, _ ->
    object : Statement() {
        override fun evaluate() {
            assumeTrue(
                "AWT/Skiko unavailable (X11 libs); Compose UI driver lifecycle tests skipped",
                runCatching { java.awt.Toolkit.getDefaultToolkit() }.isSuccess,
            )
            base.evaluate()
        }
    }
}

class DriverLifecycleTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    @Test
    fun effectStartsOncePerIntent() {
        var starts = 0
        var unitStarts = 0
        lateinit var publish: (TestMsg) -> Unit
        val driver: Driver<TestModel, TestMsg> = { p, model, content ->
            publish = p
            LaunchedEffect(Unit) { unitStarts++ }
            LaunchedEffect(model.pendingJob) {
                if (model.pendingJob == null) return@LaunchedEffect
                starts++
                p(TestMsg.JobDone)
            }
            content()
        }

        composeRule.setContent { run(testComponent, listOf(driver)) }
        composeRule.waitForIdle()

        repeat(100) { publish(TestMsg.Unrelated(it)) }
        composeRule.waitForIdle()
        assertEquals(0, starts)

        publish(TestMsg.StartJob("a"))
        composeRule.waitForIdle()
        assertEquals(1, starts)
        assertEquals(1, unitStarts)
    }

    @Test
    fun unitEffectSurvivesCallerRecomposition() {
        var unitStarts = 0
        lateinit var publish: (TestMsg) -> Unit
        val driver: Driver<TestModel, TestMsg> = { p, _, content ->
            publish = p
            LaunchedEffect(Unit) { unitStarts++ }
            content()
        }

        @Composable
        fun TestHost() {
            var bump by remember { mutableIntStateOf(0) }
            run(testComponent, listOf(driver))
            BasicText(
                text = "bump $bump",
                modifier = Modifier.clickable {
                    bump++
                    publish(TestMsg.BumpNoise(bump))
                },
            )
        }

        composeRule.setContent { TestHost() }
        composeRule.waitForIdle()
        assertEquals(1, unitStarts)

        repeat(5) {
            composeRule.onNodeWithText("bump $it").performClick()
            composeRule.waitForIdle()
        }
        assertEquals(1, unitStarts)
    }
}
