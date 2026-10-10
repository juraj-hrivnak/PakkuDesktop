/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Thread-safe model holder and stable [publish] for ELM [run].
 *
 * All message handling runs under [lock]; [publish] is a single function instance for
 * the lifetime of the store.
 *
 * [messages] is a hot [SharedFlow] with **no replay**. Each successful [publish] emits
 * after [update] runs, still inside [lock], so stream order matches applied updates.
 * Emission uses [MutableSharedFlow.tryEmit] (non-suspending). When
 * [extraBufferCapacity] is full and a collector is slow, the emit is dropped and
 * [logDroppedMessage] runs — model state is already correct; only the notification is lost.
 * Collectors must not [publish] the same message types they listen for (feedback loops).
 */
class Store<Model, Msg>(
    initial: Model,
    private val update: (Msg, Model) -> Model,
    extraBufferCapacity: Int = 64,
) {
    private val lock = Any()
    /** Referential: relies on [Component.update] returning the same instance for no-ops. */
    private val _state: MutableState<Model> = mutableStateOf(initial, referentialEqualityPolicy())
    val state: State<Model> get() = _state

    private val _messages = MutableSharedFlow<Msg>(extraBufferCapacity = extraBufferCapacity)
    val messages: SharedFlow<Msg> get() = _messages

    private val _updateError = mutableStateOf<Throwable?>(null)
    val updateError: State<Throwable?> get() = _updateError

    fun clearUpdateError() {
        _updateError.value = null
    }

    val publish: (Msg) -> Unit = { msg ->
        try {
            synchronized(lock) {
                _state.value = update(msg, _state.value)
                if (!_messages.tryEmit(msg)) {
                    logDroppedMessage(msg)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: VirtualMachineError) {
            throw e
        } catch (e: Throwable) {
            logUncaught("update", e)
            _updateError.value = e
        }
    }
}

internal fun logDroppedMessage(msg: Any?) {
    System.err.println(
        "PakkuDesktop: message stream buffer full, dropped notification for $msg",
    )
}

internal fun logUncaught(where: String, error: Throwable) {
    System.err.println("PakkuDesktop: uncaught error in $where: ${error.message}")
    error.printStackTrace()
}
