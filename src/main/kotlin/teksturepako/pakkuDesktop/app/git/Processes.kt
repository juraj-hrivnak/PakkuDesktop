/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Waits for the process to exit. If the coroutine is cancelled, destroys the process and its
 * descendants (forcibly after 2 s) before rethrowing.
 */
suspend fun Process.awaitOrDestroy(): Int = try {
    runInterruptible(Dispatchers.IO) { waitFor() }
} catch (e: CancellationException) {
    destroyTree()
    throw e
}

/**
 * Starts the process, reads its stdout to the end and waits for exit; cancellation destroys it
 * via [awaitOrDestroy]. Merge or redirect stderr on the builder so it can't fill its pipe.
 */
suspend fun ProcessBuilder.runCapturing(): Pair<Int, String> = coroutineScope {
    val proc = start()
    proc.outputStream.close()
    val output = async(Dispatchers.IO) { proc.inputStream.bufferedReader().use { it.readText() } }
    val exitCode = proc.awaitOrDestroy()
    exitCode to output.await()
}

private fun Process.destroyTree() {
    // Collected before the parent dies; afterwards its children are reparented and no longer listed.
    val descendants = descendants().toList()
    descendants.forEach { it.destroy() }
    destroy()
    if (!waitFor(2, TimeUnit.SECONDS)) destroyForcibly()
    descendants.filter { it.isAlive }.forEach { it.destroyForcibly() }
}
