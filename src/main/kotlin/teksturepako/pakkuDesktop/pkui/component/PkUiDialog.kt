/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

/** Dialog widths; always clamped to the window. */
enum class PkUiDialogSize(val width: Dp) {
    Small(420.dp),
    Medium(540.dp),
    Large(680.dp),
    ExtraLarge(820.dp),
}

/** Gap kept between the dialog and the window edges. */
private val WindowMargin = 24.dp
private val HorizontalPadding = 20.dp

/**
 * In-window modal dialog shared by every Pakku Desktop dialog: header (icon, title, subtitle,
 * close), a body bounded by the window height, and an optional footer.
 *
 * Size changes animate inside the rounded border, so corners and border stay intact while the
 * dialog grows or shrinks.
 *
 * With [scrollable] the whole body scrolls. Without it, the body is still height-bounded and
 * children can use `Modifier.weight(1f, fill = false)` for their own scroll container.
 *
 * **Esc** dismisses (when [dismissible]); **Enter** invokes [onConfirm] when provided — or only
 * **Ctrl/Cmd+Enter** with [confirmNeedsAccelerator], for dialogs with multi-line input.
 */
@Composable
fun PkUiDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: IconKey? = null,
    iconTint: Color = Color.Unspecified,
    subtitle: String? = null,
    size: PkUiDialogSize = PkUiDialogSize.Medium,
    onConfirm: (() -> Unit)? = null,
    confirmNeedsAccelerator: Boolean = false,
    dismissible: Boolean = true,
    scrollable: Boolean = true,
    initialFocus: FocusRequester? = null,
    focusKey: Any? = Unit,
    footerStart: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rootFocus = remember { FocusRequester() }
    // Re-run when the body swaps out the focused field, so Enter/Esc keep reaching the root.
    LaunchedEffect(focusKey) {
        kotlinx.coroutines.yield()
        runCatching { (initialFocus ?: rootFocus).requestFocus() }
    }
    val dismiss = { if (dismissible) onDismiss() }
    // The dialog layer's root re-provides Compose's AWT clipboard (X11-only), shadowing the
    // window's Wayland-aware one; forward the window's clipboard into the layer.
    val clipboard = LocalClipboard.current
    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        @Suppress("DEPRECATION")
        CompositionLocalProvider(
            LocalClipboard provides clipboard,
            LocalClipboardManager provides clipboardManager,
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                val width by animateDpAsState(minOf(size.width, (maxWidth - WindowMargin * 2).coerceAtLeast(280.dp)))
                val maxHeight = (maxHeight - WindowMargin * 2).coerceAtLeast(160.dp)

                ContentBox(
                    modifier = modifier
                        .focusRequester(rootFocus)
                        .focusable()
                        .dialogConfirmCancelKeys(
                            onDismiss = dismiss,
                            onConfirm = onConfirm,
                            confirmNeedsAccelerator = confirmNeedsAccelerator,
                        )
                        .width(width),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Column(
                        Modifier
                            .animateContentSize()
                            .heightIn(max = maxHeight)
                            .fillMaxWidth(),
                    ) {
                        DialogHeader(
                            title = title,
                            subtitle = subtitle,
                            icon = icon,
                            iconTint = iconTint,
                            onClose = if (dismissible) onDismiss else null,
                        )

                        if (scrollable) {
                            VerticallyScrollableContainer(
                                Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = HorizontalPadding, end = HorizontalPadding)
                                        .padding(top = 4.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    content = content,
                                )
                            }
                        } else {
                            Column(
                                Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                                    .padding(horizontal = HorizontalPadding)
                                    .padding(top = 4.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                content = content,
                            )
                        }

                        if (footer != null || footerStart != null) {
                            DialogFooter(footerStart = footerStart, footer = footer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String?,
    icon: IconKey?,
    iconTint: Color,
    onClose: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = HorizontalPadding, end = 10.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Icon(
                    key = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            Modifier.weight(1f).padding(top = 1.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(it, color = JewelTheme.contentColor.copy(alpha = 0.6f))
            }
        }
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp),
                focusable = false,
            ) {
                Icon(AllIconsKeys.General.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
private fun DialogFooter(
    footerStart: (@Composable RowScope.() -> Unit)?,
    footer: (@Composable RowScope.() -> Unit)?,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f)),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = HorizontalPadding, end = 10.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (footerStart != null) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
                content = footerStart,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        footer?.invoke(this)
    }
}

/** Standard secondary + primary buttons for [PkUiDialog]'s footer. */
@Composable
fun RowScope.PkUiDialogButtons(
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryLabel: String = "Cancel",
    onSecondary: () -> Unit,
    secondaryEnabled: Boolean = true,
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(onClick = onSecondary, enabled = secondaryEnabled) { Text(secondaryLabel) }
    extra?.invoke(this)
    DefaultButton(onClick = onPrimary, enabled = primaryEnabled) { Text(primaryLabel) }
}

enum class PkUiNoticeKind { Info, Warning, Error }

/** Inline status line with a new UI status icon. */
@Composable
fun PkUiDialogNotice(
    text: String,
    kind: PkUiNoticeKind = PkUiNoticeKind.Info,
    modifier: Modifier = Modifier,
) {
    val (icon, accent) = when (kind) {
        PkUiNoticeKind.Info -> AllIconsKeys.General.Information to JewelTheme.contentColor
        PkUiNoticeKind.Warning -> AllIconsKeys.General.Warning to PakkuDesktopConstants.amber
        PkUiNoticeKind.Error -> AllIconsKeys.General.Error to PakkuDesktopConstants.coral
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = JewelTheme.contentColor.copy(alpha = 0.9f),
        )
    }
}

/** Small muted label above a group of rows inside a dialog. */
@Composable
fun PkUiDialogSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    icon: IconKey? = null,
    trailing: String? = null,
) {
    Row(
        modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = JewelTheme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text,
            color = JewelTheme.contentColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        trailing?.let {
            Text(it, color = JewelTheme.contentColor.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

/** Rounded status chip ("new", "in pack", "3 ahead", …). */
@Composable
fun PkUiChip(
    text: String,
    muted: Boolean = false,
    accent: Color = PakkuDesktopConstants.highlightColor,
) {
    val bg = if (muted) JewelTheme.globalColors.borders.normal.copy(alpha = 0.35f) else accent.copy(alpha = 0.18f)
    val fg = if (muted) JewelTheme.contentColor.copy(alpha = 0.55f) else accent.copy(alpha = 0.95f)
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text, color = fg, fontSize = 10.sp, maxLines = 1)
    }
}
