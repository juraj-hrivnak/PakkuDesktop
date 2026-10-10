/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Target ~30 progress updates per second in high-frequency driver progress paths. */
const val PROGRESS_PUBLISH_INTERVAL_MS = 33L

/**
 * Publishes at most once per [intervalMs]; the latest value always wins and the last one is
 * never dropped while the channel stays open.
 *
 * Producers call [SendChannel.trySend]. Call [SendChannel.close] when the operation ends, then
 * [Job.join] on the returned job so the final conflated value is delivered.
 */
fun <T> CoroutineScope.conflatedPublisher(
    intervalMs: Long,
    publish: suspend (T) -> Unit,
): Pair<SendChannel<T>, Job> {
    val ch = Channel<T>(Channel.CONFLATED)
    val job = launch {
        for (v in ch) {
            publish(v)
            delay(intervalMs)
        }
    }
    return ch to job
}
