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
 *
 * @property drivers Mounted once per run, innermost in the host's driver chain, so they see
 *   the host's `CompositionLocal`s. Each driver receives only this plugin's model and a
 *   `publish` that routes back to this plugin's [Component.update]. They follow the [Driver]
 *   rules:
 *   1. Key `LaunchedEffect` on a projection of the model (ids, intent flags), never the
 *      whole model.
 *   2. After any suspension, read the latest model via `rememberUpdatedState`.
 *   3. Use `LaunchedEffect(Unit)` only for effects that don't depend on the model, such as
 *      collecting `LocalMessages`.
 *   4. Call `content` **exactly once, unconditionally**. `content` is the whole host app:
 *      skipping it blanks the window, calling it twice duplicates the app.
 *
 *   Drivers run **outside the host's error guard**. A throw during composition or from an
 *   effect can take down the app, so catch inside effects (rethrow `CancellationException`),
 *   keep the composable body free of fallible logic, and release resources in
 *   `try/finally` or `DisposableEffect`. Effects run on the main dispatcher; move heavy work
 *   to `withContext(Dispatchers.IO)` or `Dispatchers.Default`.
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
