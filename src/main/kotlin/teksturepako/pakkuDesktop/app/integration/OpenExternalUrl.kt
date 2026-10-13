/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.integration

import kotlin.concurrent.thread

private fun urlOpenCommand(url: String): List<String> {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
        os.contains("mac") -> listOf("open", url)
        else -> listOf("xdg-open", url)
    }
}

/** Opens [url] in the default browser; returns immediately (off the UI thread). */
fun openExternalUrl(url: String) {
    val command = urlOpenCommand(url)
    thread(name = "open-external-url", isDaemon = true) {
        runCatching {
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }
}
