plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("kotlinJvm") {
            id = "pakku.kotlin-jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
        register("desktopPlugin") {
            id = "pakku.desktop-plugin"
            implementationClass = "DesktopPluginConventionPlugin"
        }
        register("pluginHost") {
            id = "pakku.plugin-host"
            implementationClass = "PluginHostConventionPlugin"
        }
    }
}
