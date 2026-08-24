/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Opens the file manager on [path]'s parent and selects [path] when the platform supports it.
 * Falls back to opening the containing folder.
 *
 * Always returns immediately; file-manager calls run off the UI thread so a
 * blocking desktop/xdg-open cannot freeze the window.
 */
fun revealInFileManager(path: Path) {
    thread(name = "reveal-in-file-manager", isDaemon = true) {
        revealInFileManagerBlocking(path)
    }
}

private fun revealInFileManagerBlocking(path: Path) {
    val target = path.toAbsolutePath()
    if (!target.exists()) {
        target.parent?.let { openFolder(it) }
        return
    }
    if (target.isDirectory()) {
        openFolder(target)
        return
    }

    if (revealFile(target)) return
    target.parent?.let { openFolder(it) }
}

private fun revealFile(path: Path): Boolean {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val absolute = path.absolutePathString()
    val command = when {
        os.contains("win") -> listOf("explorer.exe", "/select,", absolute)
        os.contains("mac") -> listOf("open", "-R", absolute)
        else -> listOf("xdg-open", path.parent?.absolutePathString() ?: return false)
    }
    return startDetached(command)
}

private fun openFolder(folder: Path) {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val absolute = folder.absolutePathString()
    val command = when {
        os.contains("win") -> listOf("explorer.exe", absolute)
        os.contains("mac") -> listOf("open", absolute)
        else -> listOf("xdg-open", absolute)
    }
    startDetached(command)
}

/** Launch and forget — never waitFor, which freezes if the file manager stays attached. */
private fun startDetached(command: List<String>): Boolean = runCatching {
    ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start()
    true
}.getOrDefault(false)
