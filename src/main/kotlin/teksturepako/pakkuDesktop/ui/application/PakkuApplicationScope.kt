package teksturepako.pakkuDesktop.ui.application

import androidx.compose.ui.window.ApplicationScope
import org.jetbrains.jewel.window.DecoratedWindowScope

interface PakkuApplicationScope
{
    val applicationScope: ApplicationScope
    val decoratedWindowScope: DecoratedWindowScope
}