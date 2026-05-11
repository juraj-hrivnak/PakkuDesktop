/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.theme

import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.intui.standalone.styling.light
import org.jetbrains.jewel.ui.component.styling.MenuColors
import org.jetbrains.jewel.ui.component.styling.MenuIcons
import org.jetbrains.jewel.ui.component.styling.MenuItemColors
import org.jetbrains.jewel.ui.component.styling.MenuMetrics
import org.jetbrains.jewel.ui.component.styling.MenuStyle

/**
 * Menu [MenuStyle] for Pakku: **dark** window theme uses [PakkuDarkGlobalColors] and Gray3–5 shells;
 * **light** theme uses Jewel’s stock [MenuStyle.light] colors.
 *
 * [MenuStyle.isDark] must match the window theme’s dark flag (`ThemeDefinition.isDark`). Jewel’s
 * popup menu wraps content in `OverrideDarkMode(style.isDark)`, which overrides [JewelTheme.isDark]
 * only; [JewelTheme.globalColors] stay the window’s. If `style.isDark` disagreed with that flag,
 * anything combining `isDark` with globals would look wrong. Custom menu rows should use
 * [JewelTheme.menuStyle.colors.itemColors] for text/icons, not [JewelTheme.globalColors].
 *
 * Applied from [teksturepako.pakkuDesktop.app.ui.driver.themeDriver] via nested
 * [androidx.compose.runtime.CompositionLocalProvider] for [org.jetbrains.jewel.ui.component.styling.LocalMenuStyle].
 */
fun pakkuPopupMenuStyle(metrics: MenuMetrics, icons: MenuIcons, isDark: Boolean): MenuStyle =
    MenuStyle(
        isDark = isDark,
        colors = if (isDark) pakkuDarkPopupMenuColors() else MenuStyle.light().colors,
        metrics = metrics,
        icons = icons,
    )

private fun pakkuDarkPopupMenuColors(): MenuColors {
    val g = PakkuDarkGlobalColors
    // Gray3 — slightly elevated vs main panel (Gray2) for floating menus
    val surface = Color(0xFF2B2D30)
    val selection = Color(0xFF393B40)
    val selectionPressed = Color(0xFF43454A)
    val muted = Color(0xFF868A91)
    val mutedKeybinding = Color(0xFF6F737A)

    val itemColors =
        MenuItemColors(
            background = g.panelBackground,
            backgroundDisabled = Color.Transparent,
            backgroundFocused = selection,
            backgroundPressed = selectionPressed,
            backgroundHovered = selection,
            content = g.text.normal,
            contentDisabled = g.text.disabled,
            contentFocused = g.text.normal,
            contentPressed = g.text.normal,
            contentHovered = g.text.normal,
            iconTint = g.text.normal,
            iconTintDisabled = g.text.disabled,
            iconTintFocused = g.text.normal,
            iconTintPressed = g.text.normal,
            iconTintHovered = g.text.normal,
            keybindingTint = muted,
            keybindingTintDisabled = g.text.disabled,
            keybindingTintFocused = mutedKeybinding,
            keybindingTintPressed = mutedKeybinding,
            keybindingTintHovered = mutedKeybinding,
            separator = Color(0xFF43454A),
        )

    return MenuColors(
        background = g.panelBackground,
        border = g.borders.normal,
        shadow = Color(0x66000000),
        itemColors = itemColors,
    )
}
