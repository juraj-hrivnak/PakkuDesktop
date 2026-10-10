/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.rules.RuleChain

private data class ChildModel(val count: Int = 0)

private sealed interface ChildMsg {
    data object Inc : ChildMsg
}

private data class ParentModel(val a: ChildModel = ChildModel(), val b: ChildModel = ChildModel())

private sealed interface ParentMsg {
    data class A(val msg: ChildMsg) : ParentMsg
    data class B(val msg: ChildMsg) : ParentMsg
}

private fun childUpdate(msg: ChildMsg, model: ChildModel): ChildModel = when (msg) {
    ChildMsg.Inc -> model.copy(count = model.count + 1)
}

private fun parentUpdate(msg: ParentMsg, model: ParentModel): ParentModel = when (msg) {
    is ParentMsg.A -> model.withChild(model.a, childUpdate(msg.msg, model.a)) { copy(a = it) }
    is ParentMsg.B -> model.withChild(model.b, childUpdate(msg.msg, model.b)) { copy(b = it) }
}

@Composable
private fun ChildView(tag: String, publish: (ChildMsg) -> Unit, model: ChildModel) {
    RecomposeProbe(tag)
    BasicText("$tag ${model.count}")
}

private val testParentComponent = component(
    init = ParentModel(),
    update = ::parentUpdate,
    view = { publish, model ->
        Column {
            ChildView("childA", { publish(ParentMsg.A(it)) }, model.a)
            ChildView("childB", { publish(ParentMsg.B(it)) }, model.b)
        }
    },
)

class RunRecompositionTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private var probesWereEnabled = false

    @BeforeTest
    fun enableProbes() {
        probesWereEnabled = RecomposeCounter.enabled
        RecomposeCounter.enabled = true
    }

    @AfterTest
    fun restoreProbes() {
        RecomposeCounter.enabled = probesWereEnabled
        RecomposeCounter.reset()
    }

    @Test
    fun unrelatedMessageDoesNotRecomposeSibling() {
        lateinit var publish: (ParentMsg) -> Unit
        val captureDriver: Driver<ParentModel, ParentMsg> = { p, _, content -> publish = p; content() }

        composeRule.setContent { run(testParentComponent, listOf(captureDriver)) }
        composeRule.waitForIdle()
        RecomposeCounter.reset()

        publish(ParentMsg.A(ChildMsg.Inc))
        composeRule.waitForIdle()

        assertEquals(1, RecomposeCounter["childA"])
        assertEquals(0, RecomposeCounter["childB"])
    }
}
