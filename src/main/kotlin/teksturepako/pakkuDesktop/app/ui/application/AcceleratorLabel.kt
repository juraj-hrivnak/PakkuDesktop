/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

/**
 * Platform-aware accelerator label for Jewel menu hints (Ctrl on Linux/Windows, Cmd on macOS).
 */
fun acceleratorLabel(key: String): String
{
    val mac = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
    val prefix = if (mac) "Cmd" else "Ctrl"
    return "$prefix+$key"
}
