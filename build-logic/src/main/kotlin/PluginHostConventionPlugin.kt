import PakkuPluginDir.pluginDirAttributes
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

/**
 * The desktop app side of the plugin allowlist.
 *
 * Declare in-repo plugin modules in `dependencies { }`:
 * - `officialPlugin(project(":…"))` — ships with the app; always allowlisted.
 * - `devPlugin(project(":…"))` — allowlisted only in non-release builds.
 *
 * A build is a release build when the task graph contains a packaging task
 * (see [RELEASE_TASK]). No `-P` flag is involved, so CI and `./gradlew run` need no setup.
 *
 * `run` and tests get `build/plugins/` (every declared plugin folder) as their plugins dir.
 */
class PluginHostConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val officialPlugin = configurations.dependencyScope("officialPlugin") {
            description = "Plugin modules that ship with the app; always allowlisted."
        }
        val devPlugin = configurations.dependencyScope("devPlugin") {
            description = "In-repo plugin modules allowlisted only in non-release builds."
        }
        val officialPluginDirs = configurations.resolvable("officialPluginDirs") {
            extendsFrom(officialPlugin.get())
            attributes { pluginDirAttributes(objects) }
        }
        val devPluginDirs = configurations.resolvable("devPluginDirs") {
            extendsFrom(devPlugin.get())
            attributes { pluginDirAttributes(objects) }
        }

        val generate = tasks.register<GenerateOfficialPluginDigests>("generateOfficialPluginDigests") {
            group = "pakku"
            description = "Generates the host's plugin SHA-256 allowlist from in-repo plugin modules."
            officialPlugins.from(officialPluginDirs)
            devPlugins.from(devPluginDirs)
            releaseBuild.convention(false)
            outputDirectory.set(layout.buildDirectory.dir("generated/officialPluginDigests"))
        }
        gradle.taskGraph.whenReady {
            val release = allTasks.any { RELEASE_TASK.matches(it.name) }
            allTasks.filterIsInstance<GenerateOfficialPluginDigests>().forEach { it.releaseBuild.set(release) }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val sourceSets = extensions.getByType<SourceSetContainer>()
            sourceSets.named("main") {
                val kotlin = (this as ExtensionAware).extensions.getByName("kotlin") as SourceDirectorySet
                kotlin.srcDir(generate.flatMap { it.outputDirectory })
            }
            sourceSets.named("test") {
                compileClasspath += sourceSets.named("main").get().output
                runtimeClasspath += sourceSets.named("main").get().output
                sourceSets.findByName("dev")?.let { dev ->
                    compileClasspath += dev.output
                    runtimeClasspath += dev.output
                }
            }
            tasks.named("compileKotlin") { dependsOn(generate) }
            tasks.named("compileTestKotlin") {
                dependsOn(tasks.named("compileKotlin"), tasks.named("classes"))
                tasks.findByName("compileDevKotlin")?.let { dependsOn(it) }
            }
        }

        val stagePlugins = tasks.register<Sync>("stagePlugins") {
            group = "pakku"
            description = "Collects every declared plugin folder into build/plugins for run and tests."
            from(officialPluginDirs)
            from(devPluginDirs)
            into(layout.buildDirectory.dir("plugins"))
        }
        val stagedDir = files(stagePlugins)
        tasks.withType<JavaExec>().named { it in DEV_RUN_TASKS }.configureEach {
            jvmArgumentProviders.add(SystemPropertyDir("pakku.plugins.dir", stagedDir))
        }
        tasks.withType<Test>().configureEach {
            jvmArgumentProviders.add(SystemPropertyDir("pakku.test.pluginsDir", stagedDir))
        }
    }

    private companion object {
        /** Packaging tasks (Nucleus). Matched against the whole task graph, dependencies included. */
        val RELEASE_TASK = Regex("""create\w*Distributable|package\w*|notarize\w*|run\w*Distributable|runRelease\w*""")
        val DEV_RUN_TASKS = setOf("run", "hotRun")
    }
}

internal class SystemPropertyDir(
    @get:Input val property: String,
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) val dir: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): List<String> = listOf("-D$property=${dir.singleFile.absolutePath}")
}
