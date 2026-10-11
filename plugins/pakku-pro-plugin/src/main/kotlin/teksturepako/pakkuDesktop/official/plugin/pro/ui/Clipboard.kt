/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro.ui

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Reads plain text from the system clipboard.
 *
 * Compose Desktop / AWT only see the X11 clipboard. On Wayland, text copied from
 * native clients often never reaches AWT, and `xclip` may hold stale text, so prefer
 * `wl-paste` there. IDE / Gradle launches often omit `~/.nix-profile/bin` from PATH,
 * so `wl-paste` is also looked up in well-known locations.
 */
internal fun readClipboardText(): String?
{
    if (isWaylandSession())
    {
        resolveBinary("wl-paste")?.let { binary -> readViaProcess(listOf(binary, "-n"))?.let { return it } }
    }
    readAwtClipboardText()?.let { return it }
    return readViaProcess(listOf("xclip", "-selection", "clipboard", "-o"))
        ?: readViaProcess(listOf("xsel", "--clipboard", "--output"))
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

private fun resolveBinary(name: String): String?
{
    val onPath = System.getenv("PATH")?.split(File.pathSeparator)
        ?.map { File(it, name) }
        ?.firstOrNull { it.isFile && it.canExecute() }
        ?.absolutePath
    val home = System.getProperty("user.home")
    val candidates = listOfNotNull(
        onPath,
        home?.let { "$it/.nix-profile/bin/$name" },
        "/run/current-system/sw/bin/$name",
        "/usr/bin/$name",
        "/bin/$name",
    )
    return candidates.firstOrNull { File(it).canExecute() }
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
