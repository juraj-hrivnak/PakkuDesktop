import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named

/**
 * Variant published by `pakku.desktop-plugin` modules: a directory holding one
 * `<folder>/{manifest.edn, plugin.jar}` plugin folder, in the layout the host loader scans.
 */
internal object PakkuPluginDir {
    const val USAGE = "pakku-plugin-dir"

    fun AttributeContainer.pluginDirAttributes(objects: ObjectFactory) {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(USAGE))
    }
}
