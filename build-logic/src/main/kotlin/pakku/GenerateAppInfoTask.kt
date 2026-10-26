package pakku

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateAppInfoTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:Input
    abstract val appVersion: Property<String>

    init {
        outputDir.set(project.layout.buildDirectory.dir("generated/appInfo"))
    }

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        val file = dir.resolve("${className.get()}.kt")
        file.writeText(
            buildString {
                appendLine("package ${packageName.get()}")
                appendLine()
                appendLine("object ${className.get()} {")
                appendLine("    const val VERSION: String = \"${appVersion.get()}\"")
                appendLine("}")
            },
        )
    }
}
