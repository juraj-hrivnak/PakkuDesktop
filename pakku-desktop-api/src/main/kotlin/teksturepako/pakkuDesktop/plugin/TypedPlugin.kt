/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.plugin

import teksturepako.pakkuDesktop.elm.Component
import teksturepako.pakkuDesktop.elm.Driver
import kotlin.reflect.KClass

/**
 * A plugin's typed component and drivers, returned once from [PakkuPlugin.create].
 *
 * The host stores the model as `Any?` and checks it against [modelClass] / [msgClass]
 * once per message; plugins never see host message types.
 *
 * Rules for plugin authors:
 * - [Component.update] is pure and fast, **never calls `publish`**, and returns **the same
 *   instance** when nothing changed. The host compares models by reference, so a no-op
 *   `copy` is a real recomposition.
 * - [Component.view] must not throw. A throw during recomposition can escape the host's
 *   error guard; do parsing, indexing and casts in `update` or in [PakkuPlugin.create].
 * - [drivers] follow the [Driver] rules: key effects on a projection of the model, read
 *   the latest model via `rememberUpdatedState` after suspending, use `LaunchedEffect(Unit)`
 *   only for model-independent effects, and call `content` exactly once, unconditionally.
 *
 * @property sampleMessages Optional messages the host runs through `update`, starting from
 *   `init()`, at load time. A throw fails the plugin at load instead of in front of the user.
 */
class TypedPlugin<M : Any, S : Any>(
    val modelClass: KClass<M>,
    val msgClass: KClass<S>,
    val component: Component<M, S>,
    val drivers: List<Driver<M, S>> = emptyList(),
    val sampleMessages: List<S> = emptyList(),
)
