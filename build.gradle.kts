import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

group = "teksturepako"
version = "1.0"

/**
 * Create `github.properties` in root project folder file with
 * `gpr.usr=GITHUB_USER_ID` & `gpr.key=PERSONAL_ACCESS_TOKEN`
 **/
val githubProperties: Properties = Properties().apply {
    val properties = runCatching { FileInputStream(rootProject.file("github.properties")) }
    properties.onSuccess { load(it) }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

tasks.withType<JavaExec> {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")

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
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality

    val jewel = "243:0.27.0"

    // See https://github.com/JetBrains/Jewel/releases for the release notes
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone-$jewel")

    // Optional, for custom decorated windows:
    implementation("org.jetbrains.jewel:jewel-int-ui-decorated-window-$jewel")

    // Optional, for markdown renderer:
    implementation("org.jetbrains.jewel:jewel-markdown-int-ui-standalone-styling-$jewel")
    implementation("org.jetbrains.jewel:jewel-markdown-extension-gfm-alerts-$jewel")
    implementation("org.jetbrains.jewel:jewel-markdown-extension-autolink-$jewel")

    // Do not bring in Material (we use Jewel)
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }

    // Compose Resources
    implementation(compose.components.resources)

    // Compose Preview
    implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:1.7.1")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-swing
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // IntelliJ Icons: https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/icons
    implementation("com.jetbrains.intellij.platform:icons:243.21565.208")

    // Pakku
    implementation("teksturepako.pakku:pakku-jvm:0.21.0.164-SNAPSHOT")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // File Kit
    implementation("io.github.vinceglb:filekit-core:0.8.7")
    implementation("io.github.vinceglb:filekit-compose:0.8.7")

    // Popups
    implementation("io.github.dokar3:sonner:0.3.8")
}

compose.desktop {
    application {
        mainClass = "teksturepako.pakkupro.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
            obfuscate.set(true)
        }

        nativeDistributions {

            packageName = "PakkuPro"
            packageVersion = "1.0.0"
            vendor = "teksturepako"

            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)

            modules(
                "java.instrument",
                "java.management",
                "jdk.security.auth",
                "jdk.unsupported",
                "java.sql",
                "java.naming",
                "jdk.localedata",
            )
//
//            includeAllModules = true

            macOS {
                iconFile.set(project.file("icon.icns"))
            }

            windows {
                menuGroup = group.toString()
                shortcut = true
                iconFile.set(project.file("icon.ico"))
            }

            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}
