import PakkuPluginDir.pluginDirAttributes
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * A Pakku Desktop plugin module.
 *
 * - Compiles against the host API, the host messages/model (`:pakku-desktop-app-api`) and
 *   Compose as `compileOnly`; the host provides those
 *   classes at runtime and the loader rejects jars that bundle them.
 * - `assemblePluginDir` builds `build/plugin-dir/<name>/{plugin.jar, manifest.edn}` from the
 *   jar and `src/plugin/manifest.edn`. `<name>` is the project name without `-plugin`.
 * - Publishes that directory as the `pakku-plugin-dir` variant, which `pakku.plugin-host`
 *   consumes through `officialPlugin(...)` / `devPlugin(...)` dependencies.
 */
class DesktopPluginConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("pakku.kotlin-jvm")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        dependencies {
            "compileOnly"(project(":pakku-desktop-api"))
            "compileOnly"(project(":pakku-desktop-app-api"))
            "compileOnly"(libs.findLibrary("compose-foundation").get())
        }

        val folderName = name.removeSuffix("-plugin")
        val pluginDir = layout.buildDirectory.dir("plugin-dir")
        val assemblePluginDir = tasks.register<Sync>("assemblePluginDir") {
            group = "pakku"
            description = "Assembles the loadable plugin folder (plugin.jar + manifest.edn)."
            into(pluginDir)
            into(folderName) {
                from(tasks.named<Jar>("jar")) { rename { "plugin.jar" } }
                from("src/plugin/manifest.edn")
            }
        }

        configurations.consumable("pakkuPluginElements") {
            attributes { pluginDirAttributes(objects) }
            outgoing.artifact(pluginDir) { builtBy(assemblePluginDir) }
        }
    }
}
