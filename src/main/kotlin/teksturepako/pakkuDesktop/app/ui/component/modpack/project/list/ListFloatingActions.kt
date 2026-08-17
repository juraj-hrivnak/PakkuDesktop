/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.dialog.AddProjectsDialog
import teksturepako.pakkuDesktop.app.ui.component.dialog.RemoveProjectsDialog
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsFabAction
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun BoxScope.ListFloatingActions(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val selectedCount = model.selectedProjectKeys.size
    val busy = model.actionName != null
    val canRemove = selectedCount > 0 && !busy && model.lockFile?.get() != null

    val palette = JewelTheme.colorPalette
    val addAccent = PakkuDesktopConstants.highlightColor
    val removeAccent = palette.red.getOrNull(palette.red.lastIndex / 2)
        ?: PakkuDesktopConstants.coral
    val updateAccent = PakkuDesktopConstants.amber
    val canUpdate = selectedCount > 0 && !busy
    val lastFab = model.lastProjectsFab

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.ProjectsFabRemembered(ProjectsFabAction.Add))
                publish(ModpackMsg.ShowAddDialog)
            },
            enabled = !busy,
            buttonSize = 44.dp,
            accent = addAccent,
            isDefault = lastFab == ProjectsFabAction.Add,
        ) { _ ->
            Icon(
                key = AllIconsKeys.General.InlineAdd,
                contentDescription = "Add projects",
                tint = if (busy) Color.Gray else JewelTheme.contentColor,
                hints = arrayOf(),
                modifier = Modifier.size(22.dp),
            )
        }

        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.ProjectsFabRemembered(ProjectsFabAction.Update))
                publish(ModpackMsg.UpdateRequested(model.selectedProjectKeys))
            },
            enabled = canUpdate,
            buttonSize = 44.dp,
            accent = updateAccent,
            isDefault = lastFab == ProjectsFabAction.Update,
        ) { hovered ->
            Icon(
                key = AllIconsKeys.Actions.CheckOut,
                contentDescription = "Update selected projects",
                tint = when {
                    !canUpdate -> Color.Gray
                    hovered || lastFab == ProjectsFabAction.Update -> updateAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }

        FloatingActionIcon(
            onClick = {
                publish(ModpackMsg.ProjectsFabRemembered(ProjectsFabAction.Remove))
                publish(ModpackMsg.ShowRemoveDialog)
            },
            enabled = canRemove,
            buttonSize = 44.dp,
            accent = removeAccent,
            isDefault = lastFab == ProjectsFabAction.Remove,
        ) { hovered ->
            Icon(
                key = AllIconsKeys.General.Delete,
                contentDescription = "Remove selected projects",
                tint = when {
                    !canRemove -> Color.Gray
                    hovered || lastFab == ProjectsFabAction.Remove -> removeAccent
                    else -> JewelTheme.contentColor
                },
                hints = arrayOf(),
                modifier = Modifier.size(20.dp),
            )
        }
    }

    AddProjectsDialog(
        publish = publish,
        model = model,
    )

    RemoveProjectsDialog(
        publish = publish,
        model = model,
    )
}

/**
 * Per-button hover (not layout-coupled). Scale is graphicsLayer-only so siblings
 * stay put. Outline/glow use [accent] only while hovered/pressed (same pattern as
 * [teksturepako.pakkuDesktop.pkui.component.PkUiDropdown]).
 *
 * [isDefault] keeps a Jewel-style default-button accent ring so Enter can re-fire it.
 */
@Composable
private fun FloatingActionIcon(
    onClick: () -> Unit,
    enabled: Boolean,
    buttonSize: Dp,
    accent: Color,
    isDefault: Boolean = false,
    content: @Composable (hovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val panelBackground = animatedColor(JewelTheme.globalColors.panelBackground)
    val shadowColor = animatedColor(JewelTheme.globalColors.borders.disabled)
    val restingBorder = animatedColor(JewelTheme.globalColors.borders.normal)

    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.92f
            isHovered -> 1.14f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            isPressed -> 0.35f
            isHovered -> 0.25f
            isDefault -> 0.12f
            else -> 0f
        },
    )

    val animatedGlowPosition by animateOffsetAsState(
        targetValue = if (isHovered && enabled) mousePosition else Offset(boxSize.x / 2, boxSize.y / 2),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 120f,
        ),
    )

    val showAccentOutline = enabled && (isHovered || isPressed || isDefault)

    Box(
        modifier = Modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                alpha = if (enabled) 1f else 0.55f
            }
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> {
                                val position = event.changes.first().position
                                mousePosition = position
                                boxSize = Offset(size.width.toFloat(), size.height.toFloat())
                            }
                            PointerEventType.Exit -> {
                                mousePosition = Offset(boxSize.x / 2, boxSize.y / 2)
                            }
                        }
                    }
                }
            }
            .shadow(
                elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = Color.Transparent,
            )
            .clip(CircleShape)
            .background(panelBackground.copy(alpha = 0.94f))
            .drawBehind {
                // Resting ring — keeps the FAB readable over the list when idle
                drawCircle(
                    color = restingBorder.copy(alpha = if (enabled) 0.85f else 0.4f),
                    style = Stroke(width = 1f),
                )

                if (showAccentOutline) {
                    drawCircle(
                        color = accent.copy(
                            alpha = when {
                                isPressed -> 0.4f
                                isHovered -> 0.25f
                                else -> 0.55f // default / remembered
                            },
                        ),
                        style = Stroke(width = if (isDefault && !isHovered) 2f else 1.5f),
                    )
                }

                if (glowAlpha > 0f) {
                    val center = Offset(
                        size.width / 2 + (animatedGlowPosition.x - size.width / 2) * 0.3f,
                        size.height / 2 + (animatedGlowPosition.y - size.height / 2) * 0.3f,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = glowAlpha),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.maxDimension / 1.2f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(showAccentOutline)
    }
}
