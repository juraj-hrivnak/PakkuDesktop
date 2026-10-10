/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Debug only: composition counts per [RecomposeProbe] tag. Enable with `-Dpakku.debug.recompose=true`. */
object RecomposeCounter {
    @Volatile
    var enabled: Boolean = System.getProperty("pakku.debug.recompose").toBoolean()

    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    fun hit(tag: String) {
        counts.getOrPut(tag) { AtomicInteger() }.incrementAndGet()
    }

    fun reset() = counts.clear()

    operator fun get(tag: String): Int = counts[tag]?.get() ?: 0

    fun printAndReset() {
        println("RecomposeCounter: " + counts.entries.sortedBy { it.key }.joinToString { "${it.key}=${it.value.get()}" })
        reset()
    }
}

/**
 * Counts compositions of the *calling* scope under [tag].
 *
 * Must stay `inline`: a regular composable with a constant [tag] would itself be skipped.
 */
@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun RecomposeProbe(tag: String) {
    if (RecomposeCounter.enabled) SideEffect { RecomposeCounter.hit(tag) }
}
