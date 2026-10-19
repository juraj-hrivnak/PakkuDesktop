/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessesTest {
    @Test
    fun awaitOrDestroyKillsProcessOnCancel() = runBlocking {
        val proc = ProcessBuilder("sleep", "60")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        try {
            val started = CompletableDeferred<Unit>()
            val job = launch(Dispatchers.IO) {
                started.complete(Unit)
                proc.awaitOrDestroy()
            }
            assertTrue(proc.isAlive)
            started.await()
            job.cancelAndJoin()
            assertTrue(proc.waitFor(3, TimeUnit.SECONDS))
            assertFalse(proc.isAlive)
        } finally {
            proc.destroyForcibly()
        }
    }
}
