/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Directory holding one folder per plugin.
 *
 * `-Dpakku.plugins.dir=…` overrides it; otherwise `PakkuDesktop/plugins` under the OS
 * config dir (`$XDG_CONFIG_HOME` or `~/.config`, `%APPDATA%`, `~/Library/Application Support`).
 */
fun pluginsRoot(): Path {
    System.getProperty("pakku.plugins.dir")?.let { return Path(it) }
    val os = System.getProperty("os.name").lowercase()
    val home = Path(System.getProperty("user.home"))
    val config = when {
        "win" in os -> System.getenv("APPDATA")?.let(::Path) ?: home.resolve("AppData/Roaming")
        "mac" in os -> home.resolve("Library/Application Support")
        else -> System.getenv("XDG_CONFIG_HOME")?.let(::Path) ?: home.resolve(".config")
    }
    return config.resolve("PakkuDesktop/plugins")
}
