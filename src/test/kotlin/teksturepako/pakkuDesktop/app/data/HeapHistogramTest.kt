/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeapHistogramTest {
    @Test
    fun parseKeepsLargestRowsUpToCap() {
        val raw = buildString {
            appendLine(" num     #instances         #bytes  class name")
            appendLine("-------------------------------------------------------")
            repeat(MAX_HEAP_HISTOGRAM_ROWS + 5) { i ->
                val n = i + 1
                appendLine("   $n:          ${1000 - i}        ${10_000 - i}  demo.C$n")
            }
            appendLine("Total          99999       1234567")
        }
        val histogram = parseClassHistogram(raw)
        assertEquals(MAX_HEAP_HISTOGRAM_ROWS, histogram.rows.size)
        assertEquals("demo.C1", histogram.rows.first().className)
        assertEquals("demo.C$MAX_HEAP_HISTOGRAM_ROWS", histogram.rows.last().className)
        assertEquals(99_999, histogram.totalInstances)
        assertEquals(1_234_567, histogram.totalBytes)
        assertNull(histogram.error)
    }

    @Test
    fun formatHeapBytesUsesMbAndKb() {
        assertEquals("12.3 MB", formatHeapBytes(12_300_000))
        assertEquals("4.5 KB", formatHeapBytes(4_500))
        assertEquals("12 B", formatHeapBytes(12))
    }

    @Test
    fun shortClassNameDropsPackageAndModule() {
        assertEquals("String", shortClassName("java.lang.String (java.base@25)"))
        assertEquals("ToastData", shortClassName("teksturepako.pakkuDesktop.pkui.component.toast.ToastData"))
    }

    @Test
    fun decodeJvmDescriptors() {
        assertEquals("byte[]", decodeJvmTypeName("[B"))
        assertEquals("char[]", decodeJvmTypeName("[C"))
        assertEquals("java.lang.String[]", decodeJvmTypeName("[Ljava.lang.String;"))
        assertEquals("byte[][]", decodeJvmTypeName("[[B"))
        assertEquals("java.lang.String", decodeJvmTypeName("java.lang.String (java.base@25)"))
        assertEquals("byte[]", shortClassName("[B"))
        assertEquals("String[]", shortClassName("[Ljava.lang.String;"))
        assertEquals("Object[][]", shortClassName("[[Ljava.lang.Object;"))
    }

    @Test
    fun parseVmRssFromProcStatus() {
        val status = """
            Name:	java
            VmRSS:	  412356 kB
            VmSize:	 1999999 kB
        """.trimIndent()
        assertEquals(412356L * 1024, parseVmRssBytes(status))
    }

    @Test
    fun copySummaryIncludesDecodedNames() {
        val histogram = HeapHistogram(
            rows = persistentListOf(
                HeapHistogramRow("[B", 10, 3_800_000),
                HeapHistogramRow("java.lang.String", 20, 1_200_000),
            ),
            totalBytes = 5_000_000,
        )
        val text = histogram.copySummary()
        assertEquals(true, text.contains("byte[]"))
        assertEquals(true, text.contains("3.8 MB"))
        assertEquals(true, text.contains("String"))
    }

    @Test
    fun copyHistoryIncludesActionColumn() {
        val first = HeapFootprint(rssBytes = 400_000_000, heapUsedBytes = 40_000_000, epochMs = 1_000)
            .withHistoryFrom(null, null)
        val next = HeapFootprint(rssBytes = 420_000_000, heapUsedBytes = 50_000_000, epochMs = 6_000)
            .withHistoryFrom(first, "Exporting")
        val text = next.copyHistory()
        assertEquals(true, text.contains("Exporting"))
        assertEquals(true, text.contains("age\tRSS\tJava\taction"))
        assertEquals(true, text.contains("420.0 MB"))
    }

    @Test
    fun heapAxisTicksAreNiceAndCoverMax() {
        val ticks = heapAxisTicks(412_000_000)
        assertEquals(0L, ticks.first())
        assertEquals(true, ticks.last() >= 412_000_000)
        assertEquals("0", formatHeapAxis(0))
        assertEquals("100 MB", formatHeapAxis(100_000_000))
        assertEquals("now", formatHeapChartAge(1_000, 1_500))
        assertEquals("45s", formatHeapChartAge(0, 45_000))
        assertEquals("3m", formatHeapChartAge(0, 180_000))
    }

    @Test
    fun historyKeepsNewestUpToCap() {
        val first = HeapFootprint(rssBytes = 1, heapUsedBytes = 1, epochMs = 1).withHistoryFrom(null)
        val last = (2..(MAX_HEAP_SAMPLES + 3)).fold(first) { prev, i ->
            HeapFootprint(rssBytes = i.toLong(), heapUsedBytes = i.toLong(), epochMs = i.toLong())
                .withHistoryFrom(prev)
        }
        assertEquals(MAX_HEAP_SAMPLES, last.history.size)
        assertEquals(4L, last.history.first().totalBytes)
        assertEquals((MAX_HEAP_SAMPLES + 3).toLong(), last.history.last().totalBytes)
    }
}
