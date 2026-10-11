import java.util.Properties

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

/**
 * Create `github.properties` in root project folder file with:
 * ```properties
 * gpr.usr=GITHUB_USER_ID
 * gpr.key=PERSONAL_ACCESS_TOKEN
 * ```
 **/
val githubProperties = Properties().apply {
    rootDir.resolve("github.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://www.jetbrains.com/intellij-repository/releases")

        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        // Pakku
        maven {
            url = uri("https://maven.pkg.github.com/juraj-hrivnak/Pakku")
            credentials {
                username = githubProperties["gpr.usr"] as String? ?: System.getenv("GITHUB_ACTOR")
                password = githubProperties["gpr.key"] as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }

        google()
    }
}

// Due to an IntelliJ bug, this has to be done
rootProject.name = rootProject.projectDir.name

include(":pakku-desktop-api", ":pakku-desktop-app-api", ":sample-plugin", ":pakku-pro-plugin")

project(":sample-plugin").projectDir = file("plugins/sample-plugin")
project(":pakku-pro-plugin").projectDir = file("plugins/pakku-pro-plugin")
