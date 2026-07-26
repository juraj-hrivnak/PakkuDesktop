/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.*

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
 * @param update Pure function: takes a message and current model, returns new model via copy.
 * @param view Composable function describing what is visible given the current model.
 */
fun <Model, Msg> component(
    init: Model,
    update: (Msg, Model) -> Model,
    view: @Composable (publish: (Msg) -> Unit, model: Model) -> Unit,
): Component<Model, Msg> = Component(init = { init }, update = update, view = view)

/**
 * A driver — wraps the view tree and owns all side effects.
 *
 * Drivers:
 * - wrap [content] — they are the parent of the entire view tree
 * - provide [CompositionLocalProvider] values consumed by any component in the tree
 * - own all side effects: LaunchedEffect, disk, HTTP, clock
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
 */
@Composable
fun <Model, Msg> run(
    component: Component<Model, Msg>,
    drivers: List<Driver<Model, Msg>> = emptyList(),
) {
    var model by remember { mutableStateOf(component.init()) }
    fun publish(msg: Msg) {
        model = component.update(msg, model)
    }
    val view: @Composable () -> Unit = { component.view(::publish, model) }
    drivers.foldRight(view) { driver, inner ->
        { driver(::publish, model) { inner() } }
    }()
}

