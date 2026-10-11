/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro

import kotlinx.coroutines.test.runTest
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private class FakeBackend(
    private val activated: Boolean = false,
    private val activateError: String? = null,
) : LicenseBackend {
    val activatedKeys = mutableListOf<String>()

    override suspend fun isActivated() = activated

    override suspend fun activate(key: String): String? {
        activatedKeys += key
        return activateError
    }
}

private fun reported(activated: Boolean): AppMsg = AppMsg.Internal(InternalMsg.ProActivationReported(activated))

class LicenseDriverTest {
    private val published = mutableListOf<ProMsg>()
    private val appPublished = mutableListOf<AppMsg>()

    @Test
    fun successfulCheckReportsActivated() = runTest {
        checkActivation(FakeBackend(activated = true), { published += it }, { appPublished += it })

        assertEquals(listOf<ProMsg>(ProMsg.ActivationChecked(true)), published)
        assertEquals(listOf(reported(true)), appPublished)
    }

    @Test
    fun failedCheckReportsNotActivated() = runTest {
        checkActivation(FakeBackend(activated = false), { published += it }, { appPublished += it })

        assertEquals(listOf(reported(false)), appPublished)
    }

    @Test
    fun successfulSubmitReportsActivated() = runTest {
        val backend = FakeBackend()
        submitLicenseKey("KEY", backend, { published += it }, { appPublished += it })

        assertEquals(listOf("KEY"), backend.activatedKeys)
        assertEquals(listOf<ProMsg>(ProMsg.KeyHandled(error = null)), published)
        assertEquals(listOf(reported(true)), appPublished)
    }

    @Test
    fun failedSubmitKeepsHostFlagAndShowsError() = runTest {
        submitLicenseKey("KEY", FakeBackend(activateError = "License key not found."), { published += it }, { appPublished += it })

        assertEquals(listOf<ProMsg>(ProMsg.KeyHandled(error = "License key not found.")), published)
        assertEquals(emptyList(), appPublished)
    }

    @Test
    fun failedSubmitKeepsPreviousActivatedInModel() {
        val model = ProModel(activated = false, pendingKey = "KEY")
        val next = proUpdate(ProMsg.KeyHandled(error = "nope"), model)
        assertEquals(ProModel(activated = false, pendingKey = null, error = "nope"), next)
    }

    @Test
    fun submitWhileBusyIsNoOp() {
        val model = ProModel(activated = false, pendingKey = "KEY")
        assertSame(model, proUpdate(ProMsg.SubmitKey("OTHER"), model))
    }

    @Test
    fun sameActivationCheckIsNoOp() {
        val model = ProModel(activated = true)
        assertSame(model, proUpdate(ProMsg.ActivationChecked(true), model))
    }
}
