plugins {
    id("pakku.kotlin-jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
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
