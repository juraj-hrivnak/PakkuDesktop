/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreRaceTest {
    private data class M(val count: Int = 0, val seen: Set<Int> = emptySet())

    @Test
    fun concurrentPublishIsDeterministic() = repeat(50) {
        val store = Store<M, Int>(
            initial = M(),
            update = { msg, m -> m.copy(count = m.count + 1, seen = m.seen + msg) },
        )
        runBlocking(Dispatchers.Default) {
            (0 until 1000).map { i -> launch { store.publish(i) } }.joinAll()
        }
        assertEquals(1000, store.state.value.count)
        assertEquals((0 until 1000).toSet(), store.state.value.seen)
    }
}
