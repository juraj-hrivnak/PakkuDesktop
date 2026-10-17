/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.elm.RecomposeProbe

/**
 * Read-only [display] with an edit button that swaps in a text field. Entering selects all.
 * Enter commits (Ctrl/Cmd+Enter when [multiline]), Esc restores the value editing started
 * from, and focus loss commits. Typing commits after a short pause, so the caller's state
 * follows the field; leaving composition flushes the last edit.
 *
 * Committed text is trimmed. With [required], blank text is never committed.
 */
@Composable
fun InlineEditText(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    required: Boolean = false,
    placeholder: String = "",
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    multilineHeight: Dp = PakkuDesktopConstants.multilineInputHeight,
    editDescription: String = "Edit",
    display: @Composable RowScope.() -> Unit,
) {
    RecomposeProbe("InlineEditText")
    var editing by remember { mutableStateOf(false) }

    if (!editing) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            display()
            PkUiTooltip({ Text(editDescription) }) {
                IconButton(
                    onClick = { editing = true },
                    modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
                ) {
                    Icon(
                        AllIconsKeys.Actions.Edit,
                        editDescription,
                        tint = JewelTheme.contentColor,
                    )
                }
            }
        }
        return
    }

    InlineEditField(
        value = value,
        onCommit = onCommit,
        onClose = { editing = false },
        modifier = modifier,
        multiline = multiline,
        required = required,
        placeholder = placeholder,
        textStyle = textStyle,
        multilineHeight = multilineHeight,
    )
}

/** What one edit has sent so far; never read during composition. */
private class EditSession(val original: String)
{
    var committed: String = original
    var closed: Boolean = false
}

@Composable
private fun InlineEditField(
    value: String,
    onCommit: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    multiline: Boolean,
    required: Boolean,
    placeholder: String,
    textStyle: TextStyle,
    multilineHeight: Dp,
) {
    RecomposeProbe("InlineEditField")
    val commitLatest by rememberUpdatedState(onCommit)
    val session = remember { EditSession(value) }
    val state = remember { TextFieldState(value, TextRange(0, value.length)) }
    val focusRequester = remember { FocusRequester() }
    var gainedFocus by remember { mutableStateOf(false) }

    fun commit() {
        if (session.closed) return
        val text = state.text.toString().trim()
        if (required && text.isEmpty()) return
        if (text == session.committed) return
        session.committed = text
        commitLatest(text)
    }

    fun finish() {
        commit()
        session.closed = true
        onClose()
    }

    fun cancel() {
        if (session.closed) return
        if (session.committed != session.original) commitLatest(session.original)
        session.closed = true
        onClose()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collectLatest {
                delay(PakkuDesktopConstants.inlineEditDebounceMillis)
                commit()
            }
    }
    DisposableEffect(Unit) { onDispose { commit() } }

    val fieldModifier = modifier
        .focusRequester(focusRequester)
        .onFocusChanged { focus ->
            if (focus.hasFocus) gainedFocus = true else if (gainedFocus) finish()
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Escape -> {
                    cancel()
                    true
                }
                Key.Enter, Key.NumPadEnter ->
                    if (!multiline || event.isCtrlPressed || event.isMetaPressed) {
                        finish()
                        true
                    } else {
                        false
                    }
                else -> false
            }
        }

    if (multiline) {
        TextArea(
            state,
            fieldModifier.height(multilineHeight),
            textStyle = textStyle,
            placeholder = { Text(placeholder) },
        )
    } else {
        TextField(
            state,
            fieldModifier,
            textStyle = textStyle,
            placeholder = { Text(placeholder) },
        )
    }
}
