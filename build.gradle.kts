
import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("dev.nucleusframework") version "2.1.3"
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
    val jewel = "0.37.0-262.4852.51"

    implementation("org.jetbrains.jewel:jewel-foundation:$jewel")

    implementation("org.jetbrains.jewel:jewel-ui:$jewel")
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone:$jewel")

    // Nucleus application entry + Jewel toolkit + Tao backend (no AWT)
    val nucleus = "2.1.9"
    implementation("dev.nucleusframework:nucleus.nucleus-application:$nucleus")
    implementation("dev.nucleusframework:nucleus.decorated-window-jewel:$nucleus")
    implementation("dev.nucleusframework:nucleus.decorated-window-core:$nucleus")
    implementation("dev.nucleusframework:nucleus.decorated-window-tao:$nucleus")

    // Optional: Nucleus core runtime features (dark mode, notifications, etc.)
    implementation("dev.nucleusframework:nucleus.core-runtime:$nucleus")

    // GraalVM native-image bootstrap (fonts, resources, reachability metadata)
    implementation("dev.nucleusframework:nucleus.graalvm-runtime:$nucleus")

    // Optional: System dark-mode reactive detection
    implementation("dev.nucleusframework:nucleus.darkmode-detector:$nucleus")

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
    implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:1.11.1")

    // Navigation
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // IntelliJ Icons: https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/icons
    implementation("com.jetbrains.intellij.platform:icons:252.25557.131")

    // Pakku
    implementation("teksturepako.pakku:pakku:1.5.0.320-SNAPSHOT")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // File Kit (native dialogs — required on Tao; no AWT JFileChooser)
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
