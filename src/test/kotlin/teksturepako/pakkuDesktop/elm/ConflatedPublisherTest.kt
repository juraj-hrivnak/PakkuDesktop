/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflatedPublisherTest {

    @Test
    fun conflatedPublisher_deliversLatestAfterClose() = runTest {
        val published = mutableListOf<Int>()
        val (ch, job) = conflatedPublisher<Int>(intervalMs = 100) { published += it }
        repeat(50) { ch.trySend(it) }
        ch.close()
        job.join()
        assertEquals(1, published.size)
        assertEquals(49, published.single())
    }

    @Test
    fun conflatedPublisher_respectsIntervalBetweenPublishes() = runTest {
        val published = mutableListOf<Int>()
        val (ch, job) = conflatedPublisher<Int>(intervalMs = 100) { published += it }
        ch.trySend(1)
        advanceTimeBy(100)
        ch.trySend(2)
        advanceTimeBy(100)
        ch.close()
        job.join()
        assertEquals(listOf(1, 2), published)
    }
}
