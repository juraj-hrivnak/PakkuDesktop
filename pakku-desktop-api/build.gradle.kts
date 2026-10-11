plugins {
    id("pakku.kotlin-jvm")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    api(libs.compose.runtime)
    api(libs.kotlinx.coroutines.core)
}
