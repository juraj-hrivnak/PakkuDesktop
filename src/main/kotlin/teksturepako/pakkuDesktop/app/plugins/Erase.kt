/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.elm.Component
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.reflect.cast

/** The only place in the host that casts plugin models and messages. */
internal fun <M : Any, S : Any> TypedPlugin<M, S>.erase(): Pair<Component<Any?, Any?>, List<Driver<Any?, Any?>>> {
    val erased = Component<Any?, Any?>(
        init = { component.init() },
        update = { msg, model -> component.update(msgClass.cast(msg), modelClass.cast(model)) },
        view = { publish, model -> component.view(publish, modelClass.cast(model)) },
    )
    val erasedDrivers = drivers.map { d ->
        val e: Driver<Any?, Any?> = { publish, model, content -> d(publish, modelClass.cast(model), content) }
        e
    }
    return erased to erasedDrivers
}
