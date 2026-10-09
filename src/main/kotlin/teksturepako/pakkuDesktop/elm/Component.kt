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
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException

/**
 * A fractal component combining ELM's init/update/view.
 *
 * Always use the [component] factory — it accepts the initial model as a plain
 * value and infers both type parameters.
 */
data class Component<Model, Msg>(
    val init: () -> Model,
    val update: (Msg, Model) -> Model,
    val view: @Composable (publish: (Msg) -> Unit, model: Model) -> Unit,
)

/**
 * Factory for creating a [Component].
 * @param init The initial model value (pure, no side effects).
 * @param update Pure function: takes a message and current model, returns the next model.
 *
 * Rules for [update]:
 * - Pure and fast — no IO, clocks, randomness, or logging that allocates per message.
 * - **Never calls `publish`.** [Store.publish] uses a reentrant lock; nested publish would
 *   run an inner update and the outer write would overwrite its result.
 * - Returns **the same instance** when nothing changed (including parent reducers when a
 *   child returned its input unchanged).
 * @param view Composable function describing what is visible given the current model.
 */
fun <Model, Msg> component(
    init: Model,
    update: (Msg, Model) -> Model,
    view: @Composable (publish: (Msg) -> Unit, model: Model) -> Unit,
): Component<Model, Msg> = Component(init = { init }, update = update, view = view)

/**
 * Parent-side half of the same-instance rule: returns `this` when the child reducer
 * returned its input ([new] `===` [old]), otherwise applies [set].
 */
inline fun <P, C> P.withChild(old: C, new: C, set: P.(C) -> P): P =
    if (new === old) this else set(new)

/**
 * A driver — wraps the view tree and owns all side effects.
 *
 * Drivers:
 * - wrap [content] — they are the parent of the entire view tree
 * - provide [CompositionLocalProvider] values consumed by any component in the tree
 * - own all side effects: LaunchedEffect, disk, HTTP, clock
 *
 * Rules:
 * - 1. Key [LaunchedEffect] on a projection of the model (intent flags, ids), never the whole model.
 * - 2. After any suspension, read the latest model via [rememberUpdatedState], not the [model] captured at effect start.
 * - 3. [LaunchedEffect] `(Unit)` only for effects that do not depend on model fields.
 * - 4. Call [content] exactly once, unconditionally — no early return from the driver body before it.
 */
typealias Driver<Model, Msg> = @Composable (
    publish: (Msg) -> Unit,
    model: Model,
    content: @Composable () -> Unit,
) -> Unit

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
    chain()
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
