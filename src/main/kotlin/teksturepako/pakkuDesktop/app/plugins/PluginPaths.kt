/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import io.klogging.noCoLogger
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path

private val logger = noCoLogger("PluginPaths")
private val pluginsDirOverrideIgnored = AtomicBoolean(false)

/**
 * Directory holding one folder per plugin.
 *
 * `-Dpakku.plugins.dir=…` overrides it, except in release builds, which always use
 * `PakkuDesktop/plugins` under the OS config dir (`$XDG_CONFIG_HOME` or `~/.config`,
 * `%APPDATA%`, `~/Library/Application Support`).
 */
fun pluginsRoot(release: Boolean = PluginBuildMode.release): Path {
    System.getProperty("pakku.plugins.dir")?.let { override ->
        if (!release) {
            return Path(override)
        }
        if (pluginsDirOverrideIgnored.compareAndSet(false, true)) {
            logger.info("plugins: ignoring -Dpakku.plugins.dir in this build")
        }
    }
    val os = System.getProperty("os.name").lowercase()
    val home = Path(System.getProperty("user.home"))
    val config = when {
        "win" in os -> System.getenv("APPDATA")?.let(::Path) ?: home.resolve("AppData/Roaming")
        "mac" in os -> home.resolve("Library/Application Support")
        else -> System.getenv("XDG_CONFIG_HOME")?.let(::Path) ?: home.resolve(".config")
    }
    return config.resolve("PakkuDesktop/plugins")
}
