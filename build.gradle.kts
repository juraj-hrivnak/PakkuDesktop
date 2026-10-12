import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.TargetFormat

// Kotlin plugins are loaded here once; subprojects apply them by id through build-logic conventions.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.nucleus)
    id("pakku.kotlin-jvm")
    id("pakku.plugin-host")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

group = "teksturepako"

// -PreleaseVersion=1.1.0-beta.1 from CI; the base must equal gradle.properties `version`.
val releaseVersion: String = providers.gradleProperty("releaseVersion").orNull ?: version.toString()
version = releaseVersion

val appVersion = version.toString()
val nativeVersion = appVersion.substringBefore("-")
val debVersion = appVersion.replaceFirst("-", "~")

tasks.register("printVersion") {
    notCompatibleWithConfigurationCache("Prints version to stdout for CI")
    val v = version.toString()
    doLast { println(v) }
}

dependencies {
    // Elm runtime API + plugin SPI (what plugins compile against)
    implementation(project(":pakku-desktop-api"))
    // Host messages + model; plugins compile against it too
    implementation(project(":pakku-desktop-app-api"))

    // In-repo plugins hashed into OfficialPluginDigests (see build-logic PluginHostConventionPlugin)
    devPlugin(project(":sample-plugin"))
    officialPlugin(project(":pakku-pro-plugin"))

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
    implementation(libs.compose.components.resources)

    // Compose Preview
    implementation(libs.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.navigation.compose)

    implementation(libs.lifecycle.runtime.compose)

    // IntelliJ Icons: https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/icons
    implementation(libs.intellij.icons)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // File Kit (native dialogs — required on Tao; no AWT JFileChooser)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs.compose)

    // Logging
    implementation(libs.klogging)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.compose.ui.test.junit4)
}

tasks.test {
    useJUnit()
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
        packageVersion = appVersion
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
            packageVersion = nativeVersion
            packageBuildVersion = nativeVersion
            dmgPackageVersion = nativeVersion
        }

        windows {
            menuGroup = group.toString()
            shortcut = true
            iconFile.set(project.file("icon.ico"))
            msi.perMachine = false
            packageVersion = nativeVersion
            msiPackageVersion = nativeVersion
            exePackageVersion = nativeVersion
        }

        linux {
            iconFile.set(project.file("icon.png"))
            packageName = "pakku-desktop"

            startupWMClass = "teksturepako-PakkuDesktop"
            debPackageVersion = debVersion
            rpmPackageVersion = nativeVersion
            pacmanPackageVersion = nativeVersion
        }
    }
}
