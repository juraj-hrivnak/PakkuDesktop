/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

fun currentHeapUsedBytes(): Long =
    ManagementFactory.getMemoryMXBean().heapMemoryUsage.used

fun currentProcessRssBytes(): Long? =
    readProcSelfRssBytes() ?: readPsRssBytes()

fun parseVmRssBytes(statusText: String): Long? {
    val line = statusText.lineSequence().firstOrNull { it.startsWith("VmRSS:") } ?: return null
    val kb = line.substringAfter(':').trim().substringBefore(' ').toLongOrNull() ?: return null
    return kb * 1024
}

private fun readProcSelfRssBytes(): Long? {
    val file = File("/proc/self/status")
    if (!file.isFile) return null
    return runCatching { parseVmRssBytes(file.readText()) }.getOrNull()
}

private fun readPsRssBytes(): Long? = runCatching {
    val pid = ProcessHandle.current().pid().toString()
    val proc = ProcessBuilder("ps", "-o", "rss=", "-p", pid)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    if (!proc.waitFor(2, TimeUnit.SECONDS)) {
        proc.destroyForcibly()
        return@runCatching null
    }
    val kb = proc.inputStream.bufferedReader().use { it.readText() }.trim().toLongOrNull()
        ?: return@runCatching null
    kb * 1024
}.getOrNull()
