/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.plugin

/**
 * What a loaded plugin may do with the host's internal messages and state (credentials,
 * license). Assigned by the host from its own allowlist, never from `manifest.edn`.
 */
enum class PluginTrust {
    /** Ships with the app. Sees and publishes internal messages. */
    Official,

    /** Every other allowlisted plugin. Internal messages are hidden and dropped. */
    Standard,
}
