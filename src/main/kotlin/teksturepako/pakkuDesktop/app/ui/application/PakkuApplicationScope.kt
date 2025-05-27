/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

import androidx.compose.ui.window.ApplicationScope
import org.jetbrains.jewel.window.DecoratedWindowScope

interface PakkuApplicationScope
{
    val applicationScope: ApplicationScope
    val decoratedWindowScope: DecoratedWindowScope
}