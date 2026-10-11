/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.asAwtTransferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.integration.readClipboardText
import teksturepako.pakkuDesktop.app.integration.writeClipboardText
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * Overrides the Compose clipboard so paste/copy work when AWT cannot see the
 * Wayland clipboard (common on Linux). Must sit inside [mainWindowDriver].
 *
 * Reads prefer [readClipboardText] (`wl-paste` on Wayland). Writes go through
 * [writeClipboardText] (`wl-copy` + AWT) so native clients and in-app paste both
 * see what the copy buttons put on the clipboard.
 */
val clipboardDriver: Driver<AppModel, AppMsg> = { _, _, content ->
    val platformClipboard = LocalClipboard.current
    val clipboard = remember(platformClipboard) { FallbackClipboard(platformClipboard) }
    CompositionLocalProvider(LocalClipboard provides clipboard) {
        content()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class FallbackClipboard(
    private val delegate: Clipboard,
) : Clipboard
{
    override suspend fun getClipEntry(): ClipEntry?
    {
        val text = withContext(Dispatchers.IO) { readClipboardText() }
        if (text != null) return ClipEntry(StringSelection(text))
        return delegate.getClipEntry()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?)
    {
        if (clipEntry == null)
        {
            delegate.setClipEntry(null)
            return
        }
        val text = withContext(Dispatchers.IO) {
            clipEntry.asAwtTransferable
                ?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) }
                ?.let { it.getTransferData(DataFlavor.stringFlavor) as? String }
        }
        if (text != null)
        {
            withContext(Dispatchers.IO) { writeClipboardText(text) }
        }
        else
        {
            delegate.setClipEntry(clipEntry)
        }
    }

    override val nativeClipboard: Any
        get() = delegate.nativeClipboard
}
