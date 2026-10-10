/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class StoreMessageStreamTest {
    @Test
    fun duplicateMessageDeliveredTwice() = runTest {
        val store = Store<Int, Int>(
            initial = 0,
            update = { msg, count -> count + msg },
        )
        val collected = async(start = CoroutineStart.UNDISPATCHED) {
            store.messages.take(2).toList()
        }
        store.publish(1)
        store.publish(1)
        assertEquals(listOf(1, 1), collected.await())
    }

    @Test
    fun backToBackMessagesSameFrameInOrder() = runTest {
        val store = Store<Int, Int>(
            initial = 0,
            update = { msg, count -> count + msg },
        )
        val collected = async(start = CoroutineStart.UNDISPATCHED) {
            store.messages.take(2).toList()
        }
        store.publish(1)
        store.publish(2)
        assertEquals(listOf(1, 2), collected.await())
    }

    @Test
    fun concurrentPublishStreamMatchesUpdateOrder() = runTest {
        val appliedOrder = Collections.synchronizedList(mutableListOf<Int>())
        val store = Store<Int, Int>(
            initial = 0,
            update = { msg, count ->
                appliedOrder.add(msg)
                count + 1
            },
            extraBufferCapacity = 4096,
        )
        val collected = async(start = CoroutineStart.UNDISPATCHED) {
            store.messages.take(1000).toList()
        }
        withContext(Dispatchers.Default) {
            (0 until 1000).map { i -> async { store.publish(i) } }.awaitAll()
        }
        assertEquals(appliedOrder, collected.await())
    }
}
