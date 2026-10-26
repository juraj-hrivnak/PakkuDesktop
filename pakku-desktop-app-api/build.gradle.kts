plugins {
    id("pakku.kotlin-jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseVersion: String = providers.gradleProperty("releaseVersion").orNull ?: version.toString()
version = releaseVersion

val generateAppInfo by tasks.registering(pakku.GenerateAppInfoTask::class) {
    packageName.set("teksturepako.pakkuDesktop.app.data")
    className.set("AppInfo")
    appVersion.set(version.toString())
}

sourceSets.named("main") {
    kotlin.srcDir(generateAppInfo.map { it.outputDir })
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateAppInfo)
}

// The host's messages and model, shared with plugins (compileOnly on their side).
dependencies {
    api(project(":pakku-desktop-api"))
    api(libs.pakku)
    api(libs.compose.foundation)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.collections.immutable)
    api(libs.nucleus.core.runtime)
}
