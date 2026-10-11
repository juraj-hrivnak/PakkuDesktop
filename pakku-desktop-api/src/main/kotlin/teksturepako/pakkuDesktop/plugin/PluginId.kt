/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.plugin

/** Stable plugin identifier from `manifest.edn` `:id`. Used as model key and `key(id)`. */
@JvmInline
value class PluginId(val value: String)
