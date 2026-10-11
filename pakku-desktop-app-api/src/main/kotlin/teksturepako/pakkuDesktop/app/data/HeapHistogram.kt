/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Upper bound on [HeapHistogram.rows]; the snapshot is replaced, not appended. */
const val MAX_HEAP_HISTOGRAM_ROWS = 80

/** Upper bound on [HeapFootprint.history]; oldest samples drop. */
const val MAX_HEAP_SAMPLES = 40

/** Classes drawn as bars; the rest of the snapshot stays in the model for totals. */
const val MAX_HEAP_CHART_BARS = 16

@Immutable
data class HeapHistogramRow(
    val className: String,
    val instances: Long,
    val bytes: Long,
)

@Immutable
data class HeapSample(
    val epochMs: Long,
    val totalBytes: Long,
    val heapUsedBytes: Long = 0,
    val action: String? = null,
)

@Immutable
data class HeapFootprint(
    /** process resident set; null if the OS does not expose it */
    val rssBytes: Long? = null,
    /** Java heap used right now — no GC */
    val heapUsedBytes: Long = 0,
    val epochMs: Long = 0,
    /** at most [MAX_HEAP_SAMPLES], oldest first; RSS when known, else heap used */
    val history: PersistentList<HeapSample> = persistentListOf(),
) {
    val chartBytes: Long get() = rssBytes ?: heapUsedBytes
}

@Immutable
data class HeapHistogram(
    /** at most [MAX_HEAP_HISTOGRAM_ROWS], largest by bytes first */
    val rows: PersistentList<HeapHistogramRow> = persistentListOf(),
    val totalInstances: Long = 0,
    val totalBytes: Long = 0,
    val takenAtEpochMs: Long = 0,
    val error: String? = null,
)

private val HISTOGRAM_ROW = Regex("""^\s*\d+:\s+(\d+)\s+(\d+)\s+(.+)$""")
private val HISTOGRAM_TOTAL = Regex("""^Total\s+(\d+)\s+(\d+)""")

fun parseClassHistogram(raw: String, maxRows: Int = MAX_HEAP_HISTOGRAM_ROWS): HeapHistogram {
    val rows = mutableListOf<HeapHistogramRow>()
    var totalInstances = 0L
    var totalBytes = 0L
    for (line in raw.lineSequence()) {
        HISTOGRAM_TOTAL.find(line)?.let { match ->
            totalInstances = match.groupValues[1].toLong()
            totalBytes = match.groupValues[2].toLong()
            continue
        }
        val match = HISTOGRAM_ROW.find(line) ?: continue
        if (rows.size >= maxRows) continue
        rows.add(
            HeapHistogramRow(
                className = match.groupValues[3].trim(),
                instances = match.groupValues[1].toLong(),
                bytes = match.groupValues[2].toLong(),
            ),
        )
    }
    return HeapHistogram(
        rows = rows.toPersistentList(),
        totalInstances = totalInstances,
        totalBytes = totalBytes,
        takenAtEpochMs = System.currentTimeMillis(),
    )
}

fun formatHeapBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(Locale.US, bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(Locale.US, bytes / 1_000.0)
    else -> "$bytes B"
}

fun formatHeapAxis(bytes: Long): String = when {
    bytes <= 0L -> "0"
    bytes >= 1_000_000_000L && bytes % 1_000_000_000L == 0L -> "${bytes / 1_000_000_000L} GB"
    bytes >= 1_000_000_000L -> "%.1f GB".format(Locale.US, bytes / 1_000_000_000.0)
    bytes >= 1_000_000L && bytes % 1_000_000L == 0L -> "${bytes / 1_000_000L} MB"
    bytes >= 1_000_000L -> "%.1f MB".format(Locale.US, bytes / 1_000_000.0)
    bytes >= 1_000L && bytes % 1_000L == 0L -> "${bytes / 1_000L} KB"
    bytes >= 1_000L -> "%.1f KB".format(Locale.US, bytes / 1_000.0)
    else -> "$bytes B"
}

fun formatHeapChartAge(epochMs: Long, nowMs: Long): String {
    val sec = ((nowMs - epochMs).coerceAtLeast(0L)) / 1_000L
    return when {
        sec < 3L -> "now"
        sec < 60L -> "${sec}s"
        sec < 3_600L -> "${sec / 60L}m"
        else -> "${sec / 3_600L}h"
    }
}

fun heapAxisTicks(maxBytes: Long, targetCount: Int = 4): List<Long> {
    if (maxBytes <= 0L) return listOf(0L)
    val raw = maxBytes.toDouble() / targetCount.coerceAtLeast(1)
    val mag = 10.0.pow(floor(log10(raw.coerceAtLeast(1.0))))
    val residual = raw / mag
    val nice = when {
        residual <= 1.0 -> 1.0
        residual <= 2.0 -> 2.0
        residual <= 2.5 -> 2.5
        residual <= 5.0 -> 5.0
        else -> 10.0
    } * mag
    val step = nice.toLong().coerceAtLeast(1L)
    val top = ((maxBytes + step - 1) / step) * step
    return generateSequence(0L) { it + step }.takeWhile { it <= top }.toList()
}

fun formatHeapShare(bytes: Long, totalBytes: Long): String {
    if (totalBytes <= 0L) return "—"
    val pct = 100.0 * bytes / totalBytes
    return if (pct < 0.1) "<0.1%" else "%.1f%%".format(Locale.US, pct)
}

/**
 * JVM histogram names use HotSpot descriptors (`[B` = `byte[]`, `[Ljava.lang.String;` = `String[]`)
 * and may append ` (module@version)`.
 */
fun decodeJvmTypeName(raw: String): String {
    val noModule = raw.substringBefore(" (").trim()
    var dims = 0
    var i = 0
    while (i < noModule.length && noModule[i] == '[') {
        dims++
        i++
    }
    if (dims == 0) return noModule.replace('/', '.')
    val rest = noModule.substring(i)
    val element = when {
        rest.length == 1 -> primitiveTypeName(rest[0]) ?: rest
        rest.startsWith("L") && rest.endsWith(";") -> rest.substring(1, rest.length - 1).replace('/', '.')
        else -> rest.replace('/', '.')
    }
    return element + "[]".repeat(dims)
}

fun shortClassName(name: String): String {
    val decoded = decodeJvmTypeName(name)
    var dims = 0
    var core = decoded
    while (core.endsWith("[]")) {
        dims++
        core = core.removeSuffix("[]")
    }
    return core.substringAfterLast('.').substringAfterLast('/') + "[]".repeat(dims)
}

private fun primitiveTypeName(code: Char): String? = when (code) {
    'B' -> "byte"
    'C' -> "char"
    'D' -> "double"
    'F' -> "float"
    'I' -> "int"
    'J' -> "long"
    'S' -> "short"
    'Z' -> "boolean"
    else -> null
}

fun HeapFootprint.copySummary(): String = buildString {
    append(formatHeapBytes(chartBytes))
    if (rssBytes != null) {
        append('\n')
        append(formatHeapBytes(heapUsedBytes))
        append(" Java")
    }
}

fun HeapFootprint.copyHistory(): String {
    if (history.isEmpty()) return copySummary()
    val now = history.last().epochMs
    return buildString {
        appendLine("age\tRSS\tJava\taction")
        history.asReversed().forEach { sample ->
            append(((now - sample.epochMs).coerceAtLeast(0L) / 1_000L))
            append('s')
            append('\t')
            append(formatHeapBytes(sample.totalBytes))
            append('\t')
            append(if (sample.heapUsedBytes > 0L) formatHeapBytes(sample.heapUsedBytes) else "—")
            append('\t')
            append(sample.action.orEmpty())
            append('\n')
        }
    }.trimEnd()
}

fun HeapHistogram.copySummary(): String = rows.take(MAX_HEAP_CHART_BARS).joinToString("\n") { row ->
    val short = shortClassName(row.className)
    val full = decodeJvmTypeName(row.className)
    buildString {
        append(short)
        append('\t')
        append(formatHeapBytes(row.bytes))
        append('\t')
        append(formatHeapShare(row.bytes, totalBytes))
        if (full != short) {
            append('\t')
            append(full)
        }
    }
}

fun HeapFootprint.withHistoryFrom(previous: HeapFootprint?, action: String? = null): HeapFootprint {
    val sample = HeapSample(epochMs, chartBytes, heapUsedBytes, action)
    val next = (previous?.history ?: persistentListOf()).add(sample)
    val history = if (next.size <= MAX_HEAP_SAMPLES) next
    else next.subList(next.size - MAX_HEAP_SAMPLES, next.size).toPersistentList()
    return copy(history = history)
}
