/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.io.InputStream
import java.io.Reader
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

/** Stdout of one process, possibly cut off at [CappedCapture.truncated]. */
data class CappedCapture(val exitCode: Int, val text: String, val truncated: Boolean)

/**
 * Starts the process, reads its stdout to the end and waits for exit; cancellation destroys it
 * via [awaitOrDestroy]. Merge or redirect stderr on the builder so it can't fill its pipe.
 */
suspend fun ProcessBuilder.runCapturing(): Pair<Int, String> {
    val captured = runCapturingCapped(Int.MAX_VALUE)
    return captured.exitCode to captured.text
}

/**
 * Like [runCapturing], but keeps at most [maxChars] characters. Past that, the child is
 * destroyed so the rest of a huge diff never lands on the heap.
 */
suspend fun ProcessBuilder.runCapturingCapped(maxChars: Int): CappedCapture = coroutineScope {
    val proc = start()
    proc.outputStream.close()
    val output = async(Dispatchers.IO) {
        val reader = proc.inputStream.bufferedReader()
        try {
            readCapped(reader, maxChars) { proc.destroyForcibly() }
        } finally {
            try {
                reader.close()
            } catch (_: IOException) {
            }
        }
    }
    val exitCode = proc.awaitOrDestroy()
    val (text, truncated) = output.await()
    CappedCapture(exitCode, text, truncated)
}

/**
 * Reads stdout line by line without retaining the whole stream. Cancellation destroys the process.
 * [onLine] runs on the IO dispatcher.
 */
suspend fun ProcessBuilder.forEachOutputLine(onLine: (String) -> Unit): Int = coroutineScope {
    val proc = start()
    proc.outputStream.close()
    val read = async(Dispatchers.IO) {
        proc.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
    }
    val exitCode = proc.awaitOrDestroy()
    read.await()
    exitCode
}

/** Reads to EOF and drops the bytes so a piped child cannot block on a full stdout pipe. */
suspend fun InputStream.discardFully() {
    runInterruptible(Dispatchers.IO) {
        buffered().use { input ->
            val buf = ByteArray(8192)
            while (input.read(buf) >= 0) {
            }
        }
    }
}

private data class CappedText(val text: String, val truncated: Boolean)

private fun readCapped(reader: Reader, maxChars: Int, onCap: () -> Unit): CappedText {
    val sb = StringBuilder(minOf(maxChars, 8192).coerceAtLeast(0))
    val buf = CharArray(8192)
    var truncated = false
    try {
        while (true) {
            val n = reader.read(buf)
            if (n < 0) break
            val room = maxChars - sb.length
            if (n < room) {
                sb.append(buf, 0, n)
            } else {
                sb.append(buf, 0, room)
                val more = n > room || reader.read() >= 0
                if (more) {
                    truncated = true
                    onCap()
                }
                break
            }
        }
    } catch (e: IOException) {
        if (!truncated) throw e
    }
    return CappedText(sb.toString(), truncated)
}

private fun Process.destroyTree() {
    // Collected before the parent dies; afterwards its children are reparented and no longer listed.
    val descendants = descendants().toList()
    descendants.forEach { it.destroy() }
    destroy()
    if (!waitFor(2, TimeUnit.SECONDS)) destroyForcibly()
    descendants.filter { it.isAlive }.forEach { it.destroyForcibly() }
}
