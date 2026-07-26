/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.styling.DropdownStyle
import org.jetbrains.jewel.ui.disabled
import org.jetbrains.jewel.ui.theme.dropdownStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

// ---------------------------------------------------------------------------
// DropdownHost
// ---------------------------------------------------------------------------

@Stable
class DropdownHostState {
    var activeId by mutableStateOf<Any?>(null)
    val isActive: Boolean get() = activeId != null

    fun open(id: Any) { activeId = id }
    fun close() { activeId = null }
    fun switchTo(id: Any) { if (isActive) activeId = id }
}

val LocalDropdownHostState = staticCompositionLocalOf<DropdownHostState?> { null }

@Composable
fun DropdownHost(content: @Composable () -> Unit) {
    val hostState = remember { DropdownHostState() }
    CompositionLocalProvider(LocalDropdownHostState provides hostState) { content() }
}

// ---------------------------------------------------------------------------
// PkUiDropdown
// ---------------------------------------------------------------------------

@Composable
fun PkUiDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuModifier: Modifier = Modifier,
    outline: Outline = Outline.None,
    style: DropdownStyle = JewelTheme.dropdownStyle,
    menuContent: MenuScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val metrics = style.metrics
    val shape = RoundedCornerShape(metrics.cornerSize)
    val minSize = metrics.minSize
    val arrowMinSize = metrics.arrowMinSize

    val hostState = LocalDropdownHostState.current
    val dropdownId = remember { Any() }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // expanded is driven by the host when available, otherwise falls back to local state
    var localExpanded by remember { mutableStateOf(false) }
    val expanded = if (hostState != null) hostState.activeId == dropdownId else localExpanded

    fun dismiss() {
        if (hostState != null) hostState.close() else localExpanded = false
    }

    fun toggleExpanded() {
        if (hostState != null) {
            if (expanded) hostState.close() else hostState.open(dropdownId)
        } else {
            localExpanded = !localExpanded
        }
    }

    // Switch to this dropdown on hover when another is already open
    LaunchedEffect(isHovered) {
        if (isHovered) hostState?.switchTo(dropdownId)
    }

    var componentWidth by remember { mutableStateOf(0) }

    // Mouse position tracked as plain state — read in drawBehind via captured refs
    val mouseX = remember { mutableFloatStateOf(0f) }
    val mouseY = remember { mutableFloatStateOf(0f) }
    val boxW  = remember { mutableFloatStateOf(0f) }
    val boxH  = remember { mutableFloatStateOf(0f) }

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isPressed             -> 0.35f
            isHovered || expanded -> 0.25f
            else                  -> 0f
        }
    )
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f)

    val rawGlowX = if (isHovered || expanded) mouseX.floatValue else boxW.floatValue / 2f
    val rawGlowY = if (isHovered || expanded) mouseY.floatValue else boxH.floatValue / 2f
    val animatedGlowOffset by animateOffsetAsState(Offset(rawGlowX, rawGlowY))

    Box(
        modifier = modifier
            .onSizeChanged { componentWidth = it.width }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                ) { toggleExpanded() }
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .clip(shape)
                .drawBehind {
                    val cr = CornerRadius(metrics.cornerSize.toPx(size, this))
                    if (isHovered || expanded || isPressed) {
                        drawRoundRect(
                            color = PakkuDesktopConstants.highlightColor.copy(
                                alpha = if (isPressed) 0.4f else 0.25f
                            ),
                            cornerRadius = cr,
                            style = Stroke(width = 1f),
                        )
                    }
                    if (glowAlpha > 0f) {
                        val cx = size.width  / 2 + (animatedGlowOffset.x - size.width  / 2) * 0.3f
                        val cy = size.height / 2 + (animatedGlowOffset.y - size.height / 2) * 0.3f
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PakkuDesktopConstants.highlightColor.copy(alpha = glowAlpha),
                                    Color.Transparent,
                                ),
                                center = Offset(cx, cy),
                                radius = size.maxDimension / 1.2f,
                            ),
                            cornerRadius = cr,
                        )
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Move -> {
                                    val pos = event.changes.first().position
                                    mouseX.floatValue = pos.x
                                    mouseY.floatValue = pos.y
                                    boxW.floatValue = size.width.toFloat()
                                    boxH.floatValue = size.height.toFloat()
                                }
                                PointerEventType.Exit -> {
                                    mouseX.floatValue = boxW.floatValue / 2f
                                    mouseY.floatValue = boxH.floatValue / 2f
                                }
                            }
                        }
                    }
                }
                .width(IntrinsicSize.Max)
                .defaultMinSize(minSize.width, minSize.height.coerceAtLeast(arrowMinSize.height)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(style.metrics.contentPadding)
                    .padding(end = arrowMinSize.width),
                contentAlignment = Alignment.CenterStart,
                content = content,
            )
            Box(
                modifier = Modifier.size(arrowMinSize).align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center,
            ) {
                val alpha = if (enabled) 1f else 0.5f
                val colorFilter = if (enabled) null else ColorFilter.disabled()
                Icon(
                    modifier = Modifier.alpha(alpha),
                    key = style.icons.chevronDown,
                    contentDescription = "Dropdown Chevron",
                    colorFilter = colorFilter,
                )
            }
        }

        // ── Popup — always dark ───────────────────────────────────────────────
        if (expanded) {
            val density = LocalDensity.current

            PopupMenu(
                onDismissRequest = {
                    dismiss()
                    true
                },
                popupProperties = PopupProperties(focusable = false),
                modifier = menuModifier
                    .zIndex(1f)
                    .defaultMinSize(minWidth = with(density) { componentWidth.toDp() }),
                horizontalAlignment = Alignment.Start,
                content = menuContent,
            )
        }
    }
}
