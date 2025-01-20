package teksturepako.pakkupro.ui

import org.jetbrains.jewel.ui.icon.PathIconKey

object PakkuDesktopIcons
{
    val pakku = PathIconKey("icons/pakku.svg", this::class.java)
    val cube = PathIconKey("icons/cube.svg", this::class.java)
    val open = PathIconKey("icons/open.svg", this::class.java)
    val clone = PathIconKey("icons/clone.svg", this::class.java)
    val remove = PathIconKey("icons/remove.svg", this::class.java)
    val properties = PathIconKey("icons/properties.svg", this::class.java)
    val cloudDownload = PathIconKey("icons/cloud-download.svg", this::class.java)

    val darkTheme = PathIconKey("icons/theme/darkTheme.svg", this::class.java)
    val lightTheme = PathIconKey("icons/theme/lightTheme.svg", this::class.java)
    val systemTheme = PathIconKey("icons/theme/systemTheme.svg", this::class.java)

    object Modpack
    {
        val manage = PathIconKey("icons/generic.svg", this::class.java)
        val search = PathIconKey("icons/compass.svg", this::class.java)
    }

    object Platforms
    {
        val curseForge = PathIconKey("icons/platforms/curseforge_vector.svg", this::class.java)
        val gitHub = PathIconKey("icons/platforms/github_vector.svg", this::class.java)
        val modrinth = PathIconKey("icons/platforms/modrinth_vector.svg", this::class.java)
    }
}