/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import androidx.compose.runtime.Immutable
import java.lang.management.ManagementFactory

@Immutable
data class RuntimeDebugInfo(
    val jvmLabel: String,
    val vmName: String,
    val javaVersion: String,
    val vendor: String,
    val vendorVersion: String,
    val javaHome: String,
    val isJbr: Boolean,
    val maxHeapBytes: Long,
    val osName: String,
    val osArch: String,
    val osVersion: String,
    val sessionType: String,
    val nucleusBackend: String,
    val pid: Long,
    val cpus: Int,
    val vmArgs: String,
) {
    fun displayLines(): List<String> = buildList {
        add(jvmLabel)
        add(vmName)
        add("$osName $osArch · $sessionType · Nucleus $nucleusBackend")
        add("pid $pid · $cpus CPUs · max ${formatHeapBytes(maxHeapBytes)}")
        if (vmArgs.isNotBlank()) add(vmArgs)
    }

    fun copySummary(): String = buildString {
        appendLine("JVM: $jvmLabel")
        appendLine("VM: $vmName")
        appendLine("Vendor: $vendor ${vendorVersion.takeIf { it.isNotBlank() } ?: ""}".trimEnd())
        appendLine("JBR: ${if (isJbr) "yes" else "no"}")
        appendLine("Java: $javaVersion")
        appendLine("Home: $javaHome")
        appendLine("OS: $osName $osVersion $osArch")
        appendLine("Session: $sessionType")
        appendLine("Backend: Nucleus $nucleusBackend")
        appendLine("PID: $pid")
        appendLine("CPUs: $cpus")
        appendLine("Max heap: ${formatHeapBytes(maxHeapBytes)}")
        if (vmArgs.isNotBlank()) append("VM args: $vmArgs")
    }.trimEnd()
}

fun captureRuntimeDebugInfo(
    props: (String) -> String? = System::getProperty,
    env: (String) -> String? = System::getenv,
    vmArgs: List<String> = ManagementFactory.getRuntimeMXBean().inputArguments,
    maxHeapBytes: Long = Runtime.getRuntime().maxMemory(),
    pid: Long = ProcessHandle.current().pid(),
    cpus: Int = Runtime.getRuntime().availableProcessors(),
    nucleusBackend: String = "Tao",
): RuntimeDebugInfo {
    val vendor = props("java.vendor").orEmpty()
    val vendorVersion = props("java.vendor.version").orEmpty()
    val javaHome = props("java.home").orEmpty()
    val javaVersion = props("java.version").orEmpty()
    val isJbr = isJetBrainsRuntime(vendor, vendorVersion, javaHome)
    val nix = isNixJdk(vendorVersion, javaHome)
    return RuntimeDebugInfo(
        jvmLabel = jvmLabel(isJbr, vendor, vendorVersion, javaVersion, javaHome),
        vmName = props("java.vm.name").orEmpty().ifBlank { "unknown VM" },
        javaVersion = javaVersion,
        vendor = vendorDisplay(vendor, isJbr, nix),
        vendorVersion = vendorVersion,
        javaHome = javaHome,
        isJbr = isJbr,
        maxHeapBytes = maxHeapBytes,
        osName = props("os.name").orEmpty().ifBlank { "unknown OS" },
        osArch = props("os.arch").orEmpty(),
        osVersion = props("os.version").orEmpty(),
        sessionType = sessionType(env),
        nucleusBackend = nucleusBackend,
        pid = pid,
        cpus = cpus,
        vmArgs = interestingVmArgs(vmArgs),
    )
}

internal fun isJetBrainsRuntime(vendor: String, vendorVersion: String, javaHome: String): Boolean =
    vendor.contains("JetBrains", ignoreCase = true) ||
        vendorVersion.contains("JBR", ignoreCase = true) ||
        javaHome.contains("/jbr", ignoreCase = true) ||
        javaHome.contains("jetbrains-jdk", ignoreCase = true)

private fun missingVendor(value: String): Boolean =
    value.isBlank() || value.equals("N/A", ignoreCase = true)

private fun isNixJdk(vendorVersion: String, javaHome: String): Boolean =
    vendorVersion.contains("nix", ignoreCase = true) || javaHome.contains("/nix/store")

private fun vendorDisplay(vendor: String, isJbr: Boolean, nix: Boolean): String = when {
    isJbr -> "JetBrains"
    nix -> "Nix OpenJDK"
    missingVendor(vendor) -> "OpenJDK"
    else -> vendor
}

private fun jvmLabel(
    isJbr: Boolean,
    vendor: String,
    vendorVersion: String,
    javaVersion: String,
    javaHome: String,
): String {
    val version = javaVersion.ifBlank { vendorVersion }.ifBlank { "unknown" }
    return when {
        isJbr -> "JBR $version"
        vendorVersion.contains("Corretto", ignoreCase = true) || vendor.contains("Amazon", ignoreCase = true) ->
            "Corretto $version"
        vendorVersion.contains("Temurin", ignoreCase = true) -> "Temurin $version"
        vendor.contains("Oracle", ignoreCase = true) -> "Oracle $version"
        isNixJdk(vendorVersion, javaHome) -> "OpenJDK $version (Nix)"
        missingVendor(vendor) -> "OpenJDK $version"
        else -> "$vendor $version"
    }
}

private fun sessionType(env: (String) -> String?): String {
    val xdg = env("XDG_SESSION_TYPE")?.lowercase()
    return when {
        !env("WAYLAND_DISPLAY").isNullOrBlank() || xdg == "wayland" -> "Wayland"
        xdg == "x11" || !env("DISPLAY").isNullOrBlank() -> "X11"
        else -> "unknown session"
    }
}

private fun interestingVmArgs(args: List<String>): String =
    args.filter { arg ->
        arg.startsWith("-Xmx") || arg.startsWith("-Xms") ||
            arg.startsWith("-XX:+") || arg.startsWith("-XX:-") ||
            arg.startsWith("-XX:Max") || arg.startsWith("-XX:Use")
    }.take(8).joinToString(" ")
