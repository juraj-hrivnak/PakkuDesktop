pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

if (!rootDir.resolve("pakku/settings.gradle.kts").isFile) {
    throw GradleException(
        "Pakku submodule is missing. Run: git submodule update --init --recursive"
    )
}

includeBuild("pakku") {
    dependencySubstitution {
        substitute(module("teksturepako.pakku:pakku")).using(project(":pakku-core"))
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://www.jetbrains.com/intellij-repository/releases")

        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        google()
    }
}

// Due to an IntelliJ bug, this has to be done
rootProject.name = rootProject.projectDir.name

include(":pakku-desktop-api", ":pakku-desktop-app-api", ":sample-plugin", ":pakku-pro-plugin")

project(":sample-plugin").projectDir = file("plugins/sample-plugin")
project(":pakku-pro-plugin").projectDir = file("plugins/pakku-pro-plugin")
