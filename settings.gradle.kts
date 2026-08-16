pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

// Due to an IntelliJ bug, this has to be done
rootProject.name = rootProject.projectDir.name
