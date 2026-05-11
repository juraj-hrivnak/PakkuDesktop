/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.theme

import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.intui.standalone.styling.dark
import org.jetbrains.jewel.intui.standalone.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.styling.LocalMenuStyle
import org.jetbrains.jewel.ui.component.styling.MenuColors
import org.jetbrains.jewel.ui.component.styling.MenuIcons
import org.jetbrains.jewel.ui.component.styling.MenuItemColors
import org.jetbrains.jewel.ui.component.styling.MenuMetrics
import org.jetbrains.jewel.ui.component.styling.MenuStyle

/**
 * Overrides [LocalMenuStyle] for the whole app via [org.jetbrains.jewel.intui.standalone.theme.IntUiTheme]
 * `styling` argument (merged after [ComponentStyling.default]).
 *
 * Uses Pakku dark surfaces and [PakkuDarkGlobalColors] for menu text and borders. Metrics and icons
 * match Jewel’s light or dark menu presets according to [isDark] so spacing stays consistent with
 * the rest of the Int UI styling.
 */
fun pakkuMenuComponentStyling(isDark: Boolean): ComponentStyling {
    val reference = if (isDark) MenuStyle.dark() else MenuStyle.light()
    return ComponentStyling.provide(
        LocalMenuStyle provides pakkuPopupMenuStyle(reference.metrics, reference.icons),
    )
}

/**
 * Builds a [MenuStyle] with Pakku menu colors; callers supply metrics/icons (e.g. from [MenuStyle.dark]).
 */
fun pakkuPopupMenuStyle(metrics: MenuMetrics, icons: MenuIcons): MenuStyle =
    MenuStyle(
        isDark = true,
        colors = pakkuPopupMenuColors(),
        metrics = metrics,
        icons = icons,
    )

private fun pakkuPopupMenuColors(): MenuColors {
    val g = PakkuDarkGlobalColors
    // Gray3 — slightly elevated vs main panel (Gray2) for floating menus
    val surface = Color(0xFF2B2D30)
    val selection = Color(0xFF393B40)
    val selectionPressed = Color(0xFF43454A)
    val muted = Color(0xFF868A91)
    val mutedKeybinding = Color(0xFF6F737A)

    val itemColors =
        MenuItemColors(
            background = Color.Transparent,
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
        background = surface,
        border = g.borders.normal,
        shadow = Color(0x66000000),
        itemColors = itemColors,
    )
}
