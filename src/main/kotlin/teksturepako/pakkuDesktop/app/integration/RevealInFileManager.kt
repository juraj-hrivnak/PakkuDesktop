/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import java.awt.Desktop
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Opens the file manager on [path]'s parent and selects [path] when the platform supports it.
 * Falls back to opening the containing folder.
 */
fun revealInFileManager(path: Path) {
    val target = path.toAbsolutePath()
    if (!target.exists()) {
        target.parent?.let { openFolder(it) }
        return
    }
    if (target.isDirectory()) {
        openFolder(target)
        return
    }

    if (browseFileDirectory(target)) return
    if (revealViaProcess(target)) return
    target.parent?.let { openFolder(it) }
}

private fun browseFileDirectory(path: Path): Boolean = runCatching {
    if (!Desktop.isDesktopSupported()) return false
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) return false
    desktop.browseFileDirectory(path.toFile())
    true
}.getOrDefault(false)

private fun revealViaProcess(path: Path): Boolean {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val absolute = path.absolutePathString()
    val command = when {
        os.contains("win") -> listOf("explorer.exe", "/select,", absolute)
        os.contains("mac") -> listOf("open", "-R", absolute)
        else -> listOf(
            "dbus-send",
            "--session",
            "--dest=org.freedesktop.FileManager1",
            "--type=method_call",
            "/org/freedesktop/FileManager1",
            "org.freedesktop.FileManager1.ShowItems",
            "array:string:${path.toUri()}",
            "string:",
        )
    }
    return runCatching {
        val process = ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching false
        }
        // Windows explorer often returns 1 even on success.
        os.contains("win") || process.exitValue() == 0
    }.getOrDefault(false)
}

private fun openFolder(folder: Path) {
    FileKit.openFileWithDefaultApplication(PlatformFile(folder.absolutePathString()))
}
