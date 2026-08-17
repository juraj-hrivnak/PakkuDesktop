/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Reads plain text from the system clipboard.
 *
 * Compose Desktop / AWT only see the X11 clipboard. On Wayland, text copied from
 * native clients (browsers, etc.) often never reaches AWT — and X11 (`xclip`) may
 * still hold *stale* text. Prefer `wl-paste` whenever we appear to be on Wayland.
 *
 * IDE / Gradle launches often omit `~/.nix-profile/bin` from PATH, so we also
 * resolve `wl-paste` / `wl-copy` from well-known Nix locations.
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

/**
 * Writes plain text to the system clipboard.
 *
 * Always updates AWT. On Wayland also pipes through `wl-copy` so native clients
 * (and our own [readClipboardText] via `wl-paste`) see the new contents.
 */
fun writeClipboardText(text: String)
{
    writeAwtClipboardText(text)
    if (isWaylandSession())
    {
        writeViaWlCopy(text)
    }
    else
    {
        writeClipboardViaProcess(text)
    }
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

private fun writeAwtClipboardText(text: String)
{
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

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

private fun writeClipboardViaProcess(text: String)
{
    for (command in listOf(
        listOf("xclip", "-selection", "clipboard"),
        listOf("xsel", "--clipboard", "--input"),
    ))
    {
        if (writeViaProcess(command, text)) return
    }
}

private fun readViaWlPaste(): String?
{
    val binary = resolveWlBinary("wl-paste") ?: return null
    return readViaProcess(listOf(binary, "-n"))
}

private fun writeViaWlCopy(text: String)
{
    val binary = resolveWlBinary("wl-copy") ?: return
    writeViaProcess(listOf(binary, "-n"), text)
}

private fun resolveWlBinary(name: String): String?
{
    val home = System.getProperty("user.home") ?: return pathOrNull(name)
    val candidates = listOf(
        pathOrNull(name),
        "$home/.nix-profile/bin/$name",
        "/run/current-system/sw/bin/$name",
        "/usr/bin/$name",
        "/bin/$name",
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

private fun writeViaProcess(command: List<String>, text: String): Boolean = runCatching {
    val process = ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    process.outputStream.use { stream ->
        stream.write(text.toByteArray(StandardCharsets.UTF_8))
        stream.flush()
    }
    if (!process.waitFor(2, TimeUnit.SECONDS))
    {
        // wl-copy may keep a child alive to own the selection; parent should exit after stdin closes.
        process.destroyForcibly()
        return@runCatching false
    }
    process.exitValue() == 0
}.getOrDefault(false)
