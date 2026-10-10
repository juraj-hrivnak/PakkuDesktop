plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    compileOnly(project(":pakku-desktop-api"))
    compileOnly(libs.compose.foundation)
}

val assemblePluginDir by tasks.registering(Sync::class) {
    from(tasks.jar) { rename { "plugin.jar" } }
    from("src/plugin/manifest.edn")
    into(layout.buildDirectory.dir("plugin-dir/sample"))
}
