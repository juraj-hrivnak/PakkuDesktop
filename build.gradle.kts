
import io.github.kdroidfilter.nucleus.desktop.application.dsl.CompressionLevel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("io.github.kdroidfilter.nucleus") version "1.14.5"
}

group = "teksturepako"
version = "1.0"

/**
 * Create `github.properties` in root project folder file with:
 * ```properties
 * gpr.usr=GITHUB_USER_ID
 * gpr.key=PERSONAL_ACCESS_TOKEN
 * ```
 **/
val githubProperties: Properties = Properties().apply {
    val properties = runCatching { FileInputStream(rootProject.file("github.properties")) }
    properties.onSuccess { load(it) }
}

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

dependencies {
    /**
     * Pakku Desktop uses **Jewel**.
     *
     * [release notes](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/RELEASE%20NOTES.md)
     * [mvn repo](https://mvnrepository.com/artifact/org.jetbrains.jewel/jewel-foundation)
     */
    val jewel = "0.35.0-261.23567.138"

    implementation("org.jetbrains.jewel:jewel-foundation:$jewel")

    implementation("org.jetbrains.jewel:jewel-ui:$jewel")
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone:$jewel")

    // Nucleus Jewel decorated window (replaces jewel-int-ui-decorated-window)
    implementation("io.github.kdroidfilter:nucleus.decorated-window-jewel:1.14.5")
    implementation("io.github.kdroidfilter:nucleus.decorated-window-core:1.14.5")
    implementation("io.github.kdroidfilter:nucleus.decorated-window-jbr:1.14.5")

    // Optional: Nucleus core runtime features (dark mode, notifications, etc.)
    implementation("io.github.kdroidfilter:nucleus.core-runtime:1.14.5")

    // Optional: System dark-mode reactive detection
    implementation("io.github.kdroidfilter:nucleus.darkmode-detector:1.14.5")

    // Optional, for markdown renderer
    implementation("org.jetbrains.jewel:jewel-markdown-core:$jewel")
    implementation("org.jetbrains.jewel:jewel-markdown-int-ui-standalone-styling:$jewel")
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-gfm-alerts:$jewel")
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-autolink:$jewel")

    // Do not bring in Material (we use Jewel)
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }

    // Compose Resources
    implementation(compose.components.resources)

    // Compose Preview
    implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:1.7.1")

    // Navigation
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-swing
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // IntelliJ Icons: https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/icons
    implementation("com.jetbrains.intellij.platform:icons:252.25557.131")

    // Pakku
    implementation("teksturepako.pakku:pakku:1.5.0.319-SNAPSHOT")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // File Kit
    implementation("io.github.vinceglb:filekit-core:0.14.1")
    implementation("io.github.vinceglb:filekit-dialogs-compose:0.14.1")

    // Logging
    implementation("io.klogging:klogging-jvm:0.8.0")
}

nucleus.application {
    mainClass = "teksturepako.pakkuDesktop.MainKt"

    buildTypes {
        release {
            proguard {
                isEnabled = false
                optimize = true
                obfuscate.set(true)
                configurationFiles.from(project.file("proguard-rules.pro"))
                joinOutputJars.set(false)
            }
        }
    }

    nativeDistributions {
        packageName = "Pakku Desktop"
        packageVersion = "1.0.0"
        vendor = "teksturepako"
        copyright = "© teksturepako"

        // Nucleus supports 16 formats; keep parity with previous setup:
        targetFormats(
            TargetFormat.Dmg,   // macOS
            TargetFormat.Nsis,  // NSIS produces a traditional Windows installer (.exe) with full customization.
            TargetFormat.Deb    // Linux
        )

        enableAotCache = true
        compressionLevel = CompressionLevel.Maximum

        // JVM modules carry over as-is
        modules(
            "java.instrument",
            "java.management",
            "jdk.security.auth",
            "jdk.unsupported",
            "java.sql",
            "java.naming",
            "jdk.localedata",
            "java.desktop",
            "jdk.jdwp.agent",
            "java.net.http",
            "jdk.crypto.ec",
            "java.scripting",
            "jdk.accessibility",
            "java.prefs",
        )

        macOS {
            iconFile.set(project.file("icon.icns"))
            bundleID = "teksturepako.pakkuDesktop"
        }

        windows {
            menuGroup = group.toString()
            shortcut = true
            iconFile.set(project.file("icon.ico"))
            perUserInstall = true
        }

        linux {
            iconFile.set(project.file("icon.png"))
            packageName = "pakku-desktop"

            startupWMClass = "teksturepako-PakkuDesktop"
        }
    }
}
