package teksturepako.pakkuDesktop.app.ui.application.theme

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

enum class IntUiThemes
{
    Light, Dark, System;

    fun isDark() = (if (this == System) fromSystemTheme(currentSystemTheme) else this) == Dark

    companion object
    {
        fun fromSystemTheme(systemTheme: SystemTheme) = if (systemTheme == SystemTheme.LIGHT) Light else Dark
    }
}