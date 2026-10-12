/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.awt.Cursor
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.LinkUnderlineBehavior
import org.jetbrains.jewel.ui.component.styling.LocalLinkStyle
import teksturepako.pakkuDesktop.app.integration.openExternalUrl
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

private const val GITHUB_ISSUES_URL = "https://github.com/juraj-hrivnak/PakkuDesktop/issues"
private const val DISCORD_INVITE_URL = "https://discord.com/invite/dtAyqdzTMj"

private val Ink = Color(0xFF3A2F00)
private val Panel = Color(0xFF141414)

@Composable
fun AlphaLabel() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        AlphaChip(onClick = { expanded = !expanded })

        if (expanded) {
            AlphaFeedbackPopup(onDismiss = { expanded = false })
        }
    }
}

@Composable
private fun AlphaChip(onClick: () -> Unit) {
    val yellow = PakkuDesktopConstants.alphaYellow
    val chipShape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(yellow, chipShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Show alpha feedback options",
                onClick = onClick,
            )
            .semantics { contentDescription = "Pakku Desktop is in alpha" }
            .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("α", color = yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "alpha",
            color = Ink,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AlphaFeedbackPopup(onDismiss: () -> Unit) {
    val yellow = PakkuDesktopConstants.alphaYellow
    val popupShape = RoundedCornerShape(8.dp)
    val offsetY = with(LocalDensity.current) { 30.dp.roundToPx() }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .shadow(elevation = 8.dp, shape = popupShape, ambientColor = yellow.copy(alpha = 0.35f))
                .clip(popupShape)
                .background(Panel, popupShape)
                .border(width = 2.dp, color = yellow, shape = popupShape),
        ) {
            Text(
                text = "Pakku Desktop is in alpha.",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(yellow)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )

            InlineFeedbackText(
                onDismiss = onDismiss,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

private data class InlineLink(val tag: String, val start: Int, val end: Int)

/** Appends a link and records its character range for the hover hit-testing in InlineFeedbackText. */
private fun AnnotatedString.Builder.appendInlineLink(
    links: MutableList<InlineLink>,
    tag: String,
    url: String,
    styles: TextLinkStyles,
    onDismiss: () -> Unit,
    content: () -> Unit,
) {
    val start = length
    withLink(
        LinkAnnotation.Clickable(
            tag = tag,
            styles = styles,
            linkInteractionListener = {
                onDismiss()
                openExternalUrl(url)
            },
        ),
    ) { content() }
    links += InlineLink(tag, start, length)
}

/**
 * One seamless, word-wrapping block of text with inline links that behave like
 * Jewel's Link component: theme link color, hover color + underline on hover,
 * and a hand cursor while over a link.
 */
@Composable
private fun InlineFeedbackText(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jewelLinkStyle = LocalLinkStyle.current
    val bodyColor = PakkuDesktopConstants.alphaYellow.copy(alpha = 0.95f)

    // Resting link appearance — mirrors Jewel's LinkImpl.
    val normalStyles = TextLinkStyles(
        style = SpanStyle(
            color = jewelLinkStyle.colors.content,
            textDecoration = when (jewelLinkStyle.underlineBehavior) {
                LinkUnderlineBehavior.ShowAlways -> TextDecoration.Underline
                LinkUnderlineBehavior.ShowOnHover -> TextDecoration.None
            },
        ),
    )

    // Hovered link appearance — mirrors Jewel's LinkImpl.
    val hoveredStyles = TextLinkStyles(
        style = SpanStyle(
            color = jewelLinkStyle.colors.contentHovered,
            textDecoration = TextDecoration.Underline,
        ),
    )

    var hoveredTag by remember { mutableStateOf<String?>(null) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    fun linkStylesFor(tag: String) = if (hoveredTag == tag) hoveredStyles else normalStyles

    val links = mutableListOf<InlineLink>()
    val body = buildAnnotatedString {
        append("Everything you see now can change. ")
        append("We would like to hear your feedback. ")
        append("Please report bugs to: ")
        appendInlineLink(links, "github", GITHUB_ISSUES_URL, linkStylesFor("github"), onDismiss) {
            append("GitHub")
        }
        append(", or chat with us on ")
        appendInlineLink(links, "discord", DISCORD_INVITE_URL, linkStylesFor("discord"), onDismiss) {
            append("Discord")
        }
        append(".")
    }
    val currentLinks by rememberUpdatedState(links)

    // The hand cursor must only appear while the pointer is over a link.
    // pointerHoverIcon applies to the whole node bounds, so it cannot be enabled
    // per-link — apply the modifier conditionally instead.
    val linkHoverIcon = if (hoveredTag != null) {
        Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
    } else {
        Modifier
    }

    BasicText(
        text = body,
        style = JewelTheme.defaultTextStyle.merge(TextStyle(fontSize = 12.sp, color = bodyColor)),
        onTextLayout = { textLayout = it },
        modifier = modifier
            .then(linkHoverIcon)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Exit) {
                            hoveredTag = null
                            continue
                        }
                        val layout = textLayout ?: continue
                        val position = event.changes.firstOrNull()?.position ?: continue
                        val offset = layout.getOffsetForPosition(position)
                        hoveredTag = currentLinks.firstOrNull { offset in it.start until it.end }?.tag
                    }
                }
            },
    )
}
