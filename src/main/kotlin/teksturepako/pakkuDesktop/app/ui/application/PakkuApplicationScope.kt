/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.application.NucleusDecoratedWindowScope

interface PakkuApplicationScope
{
    val applicationScope: NucleusApplicationScope
    val decoratedWindowScope: NucleusDecoratedWindowScope
}
