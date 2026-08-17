/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.window

import androidx.compose.runtime.Stable
import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.application.NucleusDecoratedWindowScope
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope

/** Stable [PakkuApplicationScope] instance — window scope is assigned each frame without invalidating CompositionLocal consumers. */
@Stable
internal class RememberedPakkuScope(
    override val applicationScope: NucleusApplicationScope,
) : PakkuApplicationScope {
    override lateinit var decoratedWindowScope: NucleusDecoratedWindowScope
}
