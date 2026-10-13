/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString

private fun defaultApplicationCommand(absolutePath: String): List<String> {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        // Empty title so `start` does not treat a quoted path as the window title.
        os.contains("win") -> listOf("cmd.exe", "/c", "start", "\"\"", absolutePath)
        os.contains("mac") -> listOf("open", absolutePath)
        else -> listOf("xdg-open", absolutePath)
    }
}

/**
 * Opens [path] in the default application for its type (or the file manager, for a directory).
 *
 * Returns immediately. The launch runs off the UI thread and does not use
 * [java.awt.Desktop], which crashes the process on Wayland.
 */
fun openWithDefaultApplication(path: Path) {
    val command = defaultApplicationCommand(path.toAbsolutePath().absolutePathString())
    thread(name = "open-with-default-application", isDaemon = true) {
        runCatching {
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }
}
