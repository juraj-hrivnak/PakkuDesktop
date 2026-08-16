/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Reads plain text from the system clipboard.
 *
 * Compose Desktop / AWT only see the X11 clipboard. On Wayland, text copied from
 * native clients (browsers, etc.) often never reaches AWT — and X11 (`xclip`) may
 * still hold *stale* text. Prefer `wl-paste` whenever we appear to be on Wayland.
 *
 * IDE / Gradle launches often omit `~/.nix-profile/bin` from PATH, so we also
 * resolve `wl-paste` from well-known Nix locations.
 */
fun readClipboardText(): String?
{
    if (isWaylandSession())
    {
        readViaWlPaste()?.let { return it }
    }
    readAwtClipboardText()?.let { return it }
    return readClipboardViaProcess()
}

private fun isWaylandSession(): Boolean
{
    val type = System.getenv("XDG_SESSION_TYPE")?.lowercase()
    return !System.getenv("WAYLAND_DISPLAY").isNullOrBlank() || type == "wayland"
}

private fun readAwtClipboardText(): String? = runCatching {
    val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return null
    if (!contents.isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    (contents.getTransferData(DataFlavor.stringFlavor) as? String)?.takeIf { it.isNotEmpty() }
}.getOrNull()

private fun readClipboardViaProcess(): String?
{
    for (command in listOf(
        listOf("xclip", "-selection", "clipboard", "-o"),
        listOf("xsel", "--clipboard", "--output"),
    ))
    {
        readViaProcess(command)?.let { return it }
    }
    return null
}

private fun readViaWlPaste(): String?
{
    val binary = resolveWlPaste() ?: return null
    return readViaProcess(listOf(binary, "-n"))
}

private fun resolveWlPaste(): String?
{
    val home = System.getProperty("user.home") ?: return pathOrNull("wl-paste")
    val candidates = listOf(
        pathOrNull("wl-paste"),
        "$home/.nix-profile/bin/wl-paste",
        "/run/current-system/sw/bin/wl-paste",
        "/usr/bin/wl-paste",
        "/bin/wl-paste",
    )
    return candidates.firstOrNull { it != null && File(it).canExecute() }
}

private fun pathOrNull(name: String): String?
{
    val path = System.getenv("PATH") ?: return null
    return path.split(File.pathSeparator)
        .asSequence()
        .map { File(it, name) }
        .firstOrNull { it.isFile && it.canExecute() }
        ?.absolutePath
}

private fun readViaProcess(command: List<String>): String? = runCatching {
    val process = ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (!process.waitFor(2, TimeUnit.SECONDS))
    {
        process.destroyForcibly()
        return@runCatching null
    }
    if (process.exitValue() != 0) return@runCatching null
    output.takeIf { it.isNotEmpty() }
}.getOrNull()
