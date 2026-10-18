import dev.nucleusframework.desktop.application.dsl.*

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

// Compose metrics/reports are opt-in: `-PcomposeCompilerReports=true`.
// When always enabled, IntelliJ/Windows builds can fail in saveMetricsTo with "Invalid file path"
// (metric filenames derive from the Kotlin module name, which may contain ':').
composeCompiler {
    if (providers.gradleProperty("composeCompilerReports").getOrElse("false").toBoolean()) {
        val out = layout.buildDirectory.dir("compose_compiler")
        reportsDestination.set(out)
        metricsDestination.set(out)
    }
}

// The default package lowercases the project name (teksturepako.pakkudesktop), which shares a class
// directory with teksturepako.pakkuDesktop on case-insensitive file systems (macOS, Windows) and hides
// every main class from compileTestKotlin.
compose.resources {
    packageOfResClass = "teksturepako.pakkuDesktop.generated.resources"
}

group = "teksturepako"

configurations.configureEach {
    exclude(group = "org.jetbrains.intellij.deps.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

// Full release id (e.g. 1.0.0-alpha.2) lives in gradle.properties; the v* tag must match printVersion (Pakku-style).
// -PreleaseVersion=… overrides for local packaging tests; CI passes the tag version on release builds.
val releaseVersion: String = providers.gradleProperty("releaseVersion").orNull ?: version.toString()
version = releaseVersion

val appVersion = version.toString()
val nativeVersion = appVersion.substringBefore("-")

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

    // Auto-update (GitHub Releases)
    implementation(libs.nucleus.updater.runtime)

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
    implementation(libs.intellij.icons) {
        exclude(group = "org.jetbrains.intellij.deps.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.reorderable)

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
        appName = "Pakku Desktop"
        packageVersion = appVersion
        description = "GUI multiplatform modpack manager for Minecraft: Java Edition"
        homepage = "https://github.com/juraj-hrivnak/PakkuDesktop"
        vendor = "teksturepako"
        copyright = "© Juraj Hrivnák"
        artifactName = $$"pakku-desktop-${version}-${os}-${arch}.${ext}"

        targetFormats(
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.AppImage,
            TargetFormat.Pacman,
            TargetFormat.Snap,
            TargetFormat.Nsis,
            TargetFormat.Msi,
            TargetFormat.Portable,
            TargetFormat.Dmg,
            TargetFormat.Zip,
        )

        enableAotCache = false
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
            appCategory = "public.app-category.developer-tools"
            dockName = "Pakku Desktop"
            dmgPackageVersion = nativeVersion
            dmg { title = $$"${productName} ${version}" }
        }

        windows {
            menuGroup = group.toString()
            shortcut = true
            iconFile.set(project.file("icon.ico"))
            upgradeUuid = "a238db66-2327-4082-87bf-b111cf37289c"
            msi.perMachine = false
            msiPackageVersion = nativeVersion
            exePackageVersion = nativeVersion
            nsis {
                oneClick = false
                perMachine = false
                allowToChangeInstallationDirectory = true
                createDesktopShortcut = true
                createStartMenuShortcut = true
                runAfterFinish = true
                deleteAppDataOnUninstall = false
            }
        }

        linux {
            iconFile.set(project.file("icon.png"))
            packageName = "pakku-desktop"
            startupWMClass = "teksturepako-PakkuDesktop"
            debMaintainer = "teksturepako <71150936+juraj-hrivnak@users.noreply.github.com>"
            debDepends = listOf("libgtk-3-0", "git")
            rpmRequires = listOf("gtk3", "libX11", "git")
            pacmanDepends = listOf("gtk3", "libx11", "git")
            debPackageVersion = appVersion
            rpmPackageVersion = nativeVersion
            pacmanPackageVersion = nativeVersion
            appImage {
                category = AppImageCategory.Development
                genericName = "Modpack Manager"
                synopsis = "GUI multiplatform modpack manager for Minecraft: Java Edition"
            }
            snap {
                name = "pakku-desktop"
                confinement = SnapConfinement.Classic
                grade = SnapGrade.Devel
                summary = "GUI multiplatform modpack manager for Minecraft: Java Edition"
                base = "core22"
            }
        }
    }
}
