/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState

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
 * - **Never calls `publish`.** `Store.publish` uses a reentrant lock; nested publish would
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
