/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

@file:Suppress("DEPRECATION") // MenuItemState / MenuItemColors: "being made private", still the only styling API

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.InternalJewelApi
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.LocalMenuController
import org.jetbrains.jewel.ui.component.MenuContent
import org.jetbrains.jewel.ui.component.MenuItemState
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Popup
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.theme.menuStyle

private const val SUBMENU_CLOSE_DELAY_MS = 200L

/** Which [pkUiSubmenu] of a menu is open; provided per open menu by [PkUiDropdown]. */
@Stable
class PkUiSubmenuState {
    var openKey by mutableStateOf<Any?>(null)
}

val LocalPkUiSubmenuState = staticCompositionLocalOf<PkUiSubmenuState?> { null }

/**
 * Drop-in for Jewel's [MenuScope.submenu]. Jewel opens submenus with `PopupProperties(focusable = true)`, which on
 * Compose Desktop also sets `consumePointerInputOutside` and blocks all input to the parent menu layer. This one keeps
 * the parent interactive.
 */
fun MenuScope.pkUiSubmenu(
    enabled: Boolean = true,
    iconKey: IconKey? = null,
    submenu: MenuScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    selectableItem(selected = false, enabled = enabled, onClick = { }) {
        PkUiSubmenuItem(enabled, iconKey, submenu, content)
    }
}

@OptIn(ExperimentalComposeUiApi::class, InternalJewelApi::class)
@Composable
private fun PkUiSubmenuItem(
    enabled: Boolean,
    iconKey: IconKey?,
    submenu: MenuScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val state = LocalPkUiSubmenuState.current ?: remember { PkUiSubmenuState() }
    val key = remember { Any() }
    val open = enabled && state.openKey === key

    var rowHovered by remember { mutableStateOf(false) }
    var popupHovered by remember { mutableStateOf(false) }

    fun close() {
        if (state.openKey === key) state.openKey = null
    }

    LaunchedEffect(rowHovered, popupHovered, enabled) {
        when {
            !enabled -> close()
            rowHovered -> state.openKey = key
            !popupHovered && state.openKey === key -> {
                delay(SUBMENU_CLOSE_DELAY_MS)
                close()
            }
        }
    }

    val style = JewelTheme.menuStyle
    val itemMetrics = style.metrics.itemMetrics
    val itemColors = style.colors.itemColors
    val itemState = MenuItemState.of(selected = open, enabled = enabled, hovered = rowHovered)
    val background by itemColors.backgroundFor(itemState)
    val contentColor by itemColors.contentFor(itemState)
    val iconTint by itemColors.iconTintFor(itemState)

    Box(
        Modifier
            .fillMaxWidth()
            .onHover { rowHovered = it }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { state.openKey = if (open) null else key }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight) {
                    state.openKey = key
                    true
                } else {
                    false
                }
            }
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = itemMetrics.minHeight)
                    .drawBehind {
                        val outer = itemMetrics.outerPadding
                        val left = outer.calculateLeftPadding(layoutDirection).toPx()
                        val top = outer.calculateTopPadding().toPx()
                        val corner = itemMetrics.selectionCornerSize.toPx(size, this)
                        drawRoundRect(
                            color = background,
                            topLeft = Offset(left, top),
                            size = Size(
                                size.width - left - outer.calculateRightPadding(layoutDirection).toPx(),
                                size.height - top - outer.calculateBottomPadding().toPx(),
                            ),
                            cornerRadius = CornerRadius(corner, corner),
                        )
                    }
                    .padding(itemMetrics.contentPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconKey != null) {
                    Icon(key = iconKey, contentDescription = null, modifier = Modifier.size(itemMetrics.iconSize))
                } else {
                    Spacer(Modifier.size(itemMetrics.iconSize))
                }
                Box(Modifier.weight(1f)) { content() }
                Icon(
                    key = style.icons.submenuChevron,
                    contentDescription = null,
                    modifier = Modifier.size(itemMetrics.iconSize),
                    tint = iconTint,
                )
            }
        }

        if (open) {
            val parentController = LocalMenuController.current
            val controller = remember(parentController, state, key) {
                parentController.submenuController {
                    if (state.openKey === key) state.openKey = null
                    true
                }
            }
            val density = LocalDensity.current
            val positionProvider = remember(style, density) {
                SubmenuPositionProvider(style.metrics.submenuMetrics.offset, style.metrics.menuMargin, density)
            }

            Popup(
                popupPositionProvider = positionProvider,
                cornerSize = style.metrics.cornerSize,
                onDismissRequest = ::close,
                properties = PopupProperties(focusable = true, consumePointerInputOutside = false),
            ) {
                DisposableEffect(Unit) { onDispose { popupHovered = false } }
                CompositionLocalProvider(LocalMenuController provides controller) {
                    Box(Modifier.onHover { popupHovered = it }) {
                        MenuContent(style = style, content = submenu)
                    }
                }
            }
        }
    }
}

/** Same placement as Jewel's internal `AnchorHorizontalMenuPositionProvider`: right of the row, flips left if needed. */
private class SubmenuPositionProvider(
    private val contentOffset: DpOffset,
    private val contentMargin: PaddingValues,
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = with(density) {
        val bounds = IntRect(
            left = contentMargin.calculateLeftPadding(layoutDirection).roundToPx(),
            top = contentMargin.calculateTopPadding().roundToPx(),
            right = windowSize.width - contentMargin.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = windowSize.height - contentMargin.calculateBottomPadding().roundToPx(),
        )
        val offsetX = contentOffset.x.roundToPx() * if (layoutDirection == LayoutDirection.Ltr) 1 else -1
        val offsetY = contentOffset.y.roundToPx()

        val rightSpace = bounds.right - anchorBounds.right
        val leftSpace = anchorBounds.left - bounds.left
        val x = if (rightSpace > popupContentSize.width || rightSpace >= leftSpace) {
            anchorBounds.right + offsetX
        } else {
            anchorBounds.left - offsetX - popupContentSize.width
        }
        val y = anchorBounds.top + offsetY + 4

        IntOffset(
            x.coerceIn(bounds.left, (bounds.right - popupContentSize.width).coerceAtLeast(bounds.left)),
            y.coerceIn(bounds.top, (bounds.bottom - popupContentSize.height).coerceAtLeast(bounds.top)),
        )
    }
}
