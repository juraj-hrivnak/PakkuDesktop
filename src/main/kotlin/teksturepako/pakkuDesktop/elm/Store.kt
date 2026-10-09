/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe model holder and stable [publish] for ELM [run].
 *
 * All message handling runs under [lock]; [publish] is a single function instance for
 * the lifetime of the store.
 */
class Store<Model, Msg>(
    initial: Model,
    private val update: (Msg, Model) -> Model,
) {
    private val lock = Any()
    private val _state: MutableState<Model> = mutableStateOf(initial)
    val state: State<Model> get() = _state

    private val _updateError = mutableStateOf<Throwable?>(null)
    val updateError: State<Throwable?> get() = _updateError

    fun clearUpdateError() {
        _updateError.value = null
    }

    val publish: (Msg) -> Unit = { msg ->
        try {
            synchronized(lock) {
                _state.value = update(msg, _state.value)
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

internal fun logUncaught(where: String, error: Throwable) {
    System.err.println("PakkuDesktop: uncaught error in $where: ${error.message}")
    error.printStackTrace()
}
