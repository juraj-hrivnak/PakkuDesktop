/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project

fun Project.hasProviderVersionMismatch(): Boolean =
    versionsDoNotMatchAcrossProviders(Provider.providers)

fun prettyLoaderName(loader: String): String = when (loader.lowercase())
{
    "forge"    -> "Forge"
    "neoforge" -> "NeoForge"
    "fabric"   -> "Fabric"
    "quilt"    -> "Quilt"
    "liteloader", "lite_loader" -> "LiteLoader"
    else       -> loader.replaceFirstChar { it.uppercase() }
}
