package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.foundation.Stroke
import org.jetbrains.jewel.foundation.modifier.border
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.foundation.theme.LocalTextStyle
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.DropdownState
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.styling.DropdownStyle
import org.jetbrains.jewel.ui.disabled
import org.jetbrains.jewel.ui.focusOutline
import org.jetbrains.jewel.ui.outline
import org.jetbrains.jewel.ui.painter.hints.Stateful
import org.jetbrains.jewel.ui.theme.dropdownStyle
import org.jetbrains.jewel.ui.util.thenIf

private var currentlyExpandedDropdownId by mutableStateOf<Int?>(null)
private var dropdownCounter = 0

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
    val dropdownId = remember { dropdownCounter++ }
    var dropdownState by remember { mutableStateOf(DropdownState.of(enabled = enabled)) }
    val expanded = currentlyExpandedDropdownId == dropdownId

    remember(enabled) { dropdownState = dropdownState.copy(enabled = enabled) }

    LaunchedEffect(expanded)
    {
        dropdownState = dropdownState.copy(hovered = expanded)
    }

    val colors = style.colors
    val metrics = style.metrics
    val shape = RoundedCornerShape(style.metrics.cornerSize)
    val minSize = metrics.minSize
    val arrowMinSize = style.metrics.arrowMinSize
    val borderColor by colors.borderFor(dropdownState)
    val hasNoOutline = outline == Outline.None

    var componentWidth by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .onSizeChanged { componentWidth = it.width }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .background(colors.backgroundFor(dropdownState).value, shape)
                .thenIf(hasNoOutline) { border(Stroke.Alignment.Center, style.metrics.borderWidth, borderColor, shape) }
                .thenIf(outline == Outline.None) { focusOutline(dropdownState, shape) }
                .outline(dropdownState, outline, shape)
                .width(IntrinsicSize.Max)
                .defaultMinSize(minSize.width, minSize.height.coerceAtLeast(arrowMinSize.height))
                .onPointerEvent(PointerEventType.Enter) {
                    if (enabled) {
                        if (currentlyExpandedDropdownId != dropdownId)
                        {
                            currentlyExpandedDropdownId = dropdownId
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Press) {
                    if (enabled)
                    {
                        currentlyExpandedDropdownId = dropdownId
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.contentFor(dropdownState).value,
                LocalTextStyle provides LocalTextStyle.current.copy(color = colors.contentFor(dropdownState).value),
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
                    val alpha = if (dropdownState.isEnabled) 1f else 0.5f
                    val colorFilter = if (dropdownState.isEnabled) null else ColorFilter.disabled()
                    Icon(
                        modifier = Modifier.alpha(alpha),
                        key = style.icons.chevronDown,
                        contentDescription = "Dropdown Chevron",
                        colorFilter = colorFilter,
                        hint = Stateful(dropdownState),
                    )
                }
            }
        }

        if (expanded) {
            val density = LocalDensity.current
            PopupMenu(
                onDismissRequest = {
                    currentlyExpandedDropdownId = null
                    true
                },
                popupProperties = PopupProperties(
                    focusable = false
                ),
                modifier = menuModifier
                    .offset(x = (-10).dp)
                    .defaultMinSize(minWidth = with(density) { componentWidth.toDp() }),
                style = style.menuStyle,
                horizontalAlignment = Alignment.Start,
                content = menuContent,
            )
        }
    }
}

// Extension function for cleaner pointer event handling
private fun Modifier.onPointerEvent(
    eventType: PointerEventType,
    handler: () -> Unit
) = this.pointerInput(eventType) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == eventType) {
                handler()
            }
        }
    }
}