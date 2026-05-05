pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("plugin.compose").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("io.github.kdroidfilter.nucleus").version(extra["nucleus.version"] as String)
    }
}

// Due to an IntelliJ bug, this has to be done
rootProject.name = rootProject.projectDir.name
