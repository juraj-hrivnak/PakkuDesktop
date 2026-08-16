import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.nucleus)
}

kotlin {
    jvmToolchain(25)
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
    // Pakku
    implementation(libs.pakku)

    /**
     * Pakku Desktop uses **Jewel**.
     *
     * [release notes](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/RELEASE%20NOTES.md)
     * [mvn repo](https://mvnrepository.com/artifact/org.jetbrains.jewel/jewel-foundation)
     */
    implementation(libs.jewel.foundation)

    implementation(libs.jewel.ui)
    implementation(libs.jewel.int.ui.standalone)

    // Nucleus application entry + Jewel toolkit + Tao backend (no AWT)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decorated.window.jewel)
    implementation(libs.nucleus.decorated.window.core)
    implementation(libs.nucleus.decorated.window.tao)

    // Optional: Nucleus core runtime features (dark mode, notifications, etc.)
    implementation(libs.nucleus.core.runtime)

    // GraalVM native-image bootstrap (fonts, resources, reachability metadata)
    implementation(libs.nucleus.graalvm.runtime)

    // Optional: System dark-mode reactive detection
    implementation(libs.nucleus.darkmode.detector)

    // Optional, for markdown renderer
    implementation(libs.jewel.markdown.core)
    implementation(libs.jewel.markdown.int.ui.standalone.styling)
    implementation(libs.jewel.markdown.extensions.gfm.alerts)
    implementation(libs.jewel.markdown.extensions.autolink)

    // Do not bring in Material (we use Jewel)
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }

    // Compose Resources
    implementation(compose.components.resources)

    // Compose Preview
    implementation(libs.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.navigation.compose)

    implementation(libs.lifecycle.runtime.compose)

    // IntelliJ Icons: https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/icons
    implementation(libs.intellij.icons)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // File Kit (native dialogs — required on Tao; no AWT JFileChooser)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs.compose)

    // Logging
    implementation(libs.klogging)
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
            msi.perMachine = false
        }

        linux {
            iconFile.set(project.file("icon.png"))
            packageName = "pakku-desktop"

            startupWMClass = "teksturepako-PakkuDesktop"
        }
    }
}
