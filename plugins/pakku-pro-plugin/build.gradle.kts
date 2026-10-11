plugins {
    id("pakku.desktop-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// compileOnly: the host's classpath already has these, and the loader rejects bundled host classes.
dependencies {
    compileOnly(libs.pakku)
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.jewel.foundation)
    compileOnly(libs.jewel.ui)

    testImplementation(project(":pakku-desktop-app-api"))
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnit()
}
