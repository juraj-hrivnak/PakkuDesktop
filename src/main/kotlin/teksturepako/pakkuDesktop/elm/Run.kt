/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a [Component] exactly once at the top level.
 *
 * Never call this inside a view.
 *
 * [update] and [view] are isolated so a bug in either one cannot take down the
 * process. Skiko logs composition failures and then rethrows; without a catch
 * here that rethrow kills the Tao event loop. Fatal VM errors still propagate.
 */
@Composable
fun <Model, Msg> run(
    component: Component<Model, Msg>,
    drivers: List<Driver<Model, Msg>> = emptyList(),
) {
    val currentComponent by rememberUpdatedState(component)
    val store = remember {
        Store<Model, Msg>(
            initial = component.init(),
            update = { msg, model -> currentComponent.update(msg, model) },
        )
    }

    val chain: @Composable () -> Unit = remember(drivers) {
        val view: @Composable () -> Unit = {
            GuardedContent(
                externalError = store.updateError.value,
                onDismissExternal = store::clearUpdateError,
            ) {
                currentComponent.view(store.publish, store.state.value)
            }
        }
        drivers.foldRight(view) { driver, inner ->
            { driver(store.publish, store.state.value, inner) }
        }
    }
    CompositionLocalProvider(LocalMessages provides store.messages) {
        chain()
    }
}

private fun Throwable.shouldPropagate(): Boolean =
    this is CancellationException || this is VirtualMachineError

/**
 * Isolates [content] in a nested composition so a throw cannot leave the parent
 * slot table half-applied. [pending] is a non-snapshot field so a failure is
 * remembered immediately and the same tree is not retried every frame.
 *
 * **Known limit:** Compose cannot try/catch around composable calls. This catches what
 * escapes [SubcomposeLayout.subcompose], but a throw during recomposition of an already
 * composed child may still escape. Treat as protection against **`update` errors** only —
 * keep [Component.view] free of fallible logic (parse, index, cast in [Component.update]
 * or at load time, not in the view tree).
 */
@Composable
private fun GuardedContent(
    externalError: Throwable?,
    onDismissExternal: () -> Unit,
    content: @Composable () -> Unit,
) {
    val pending = remember { arrayOfNulls<Throwable>(1) }
    val recompose = currentRecomposeScope
    val error = externalError ?: pending[0]
    val dismiss = {
        pending[0] = null
        onDismissExternal()
        recompose.invalidate()
    }

    if (error != null) {
        UncaughtErrorPanel(error, onDismiss = dismiss)
        return
    }

    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val measurables = try {
            subcompose("content") { content() }
        } catch (e: Throwable) {
            if (e.shouldPropagate()) throw e
            logUncaught("composition", e)
            pending[0] = e
            recompose.invalidate()
            subcompose("error") { UncaughtErrorPanel(e, onDismiss = dismiss) }
        }
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        layout(width, height) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

@Composable
private fun UncaughtErrorPanel(error: Throwable, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText("Something went wrong")
        BasicText(error.toString())
        BasicText(
            text = "Dismiss",
            modifier = Modifier.padding(top = 16.dp).clickable(onClick = onDismiss),
        )
    }
}
