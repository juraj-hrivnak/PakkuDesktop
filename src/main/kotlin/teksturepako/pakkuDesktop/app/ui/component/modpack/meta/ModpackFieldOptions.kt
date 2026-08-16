/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Multiplatform

/** Shared choice lists for modpack init + Modpack tab editors (API serial names). */
object ModpackFieldOptions {
    val LOADERS = listOf("fabric", "quilt", "forge", "neoforge")

    val TARGETS = listOf(
        CurseForge.serialName,
        Modrinth.serialName,
        Multiplatform.serialName,
    )
}
