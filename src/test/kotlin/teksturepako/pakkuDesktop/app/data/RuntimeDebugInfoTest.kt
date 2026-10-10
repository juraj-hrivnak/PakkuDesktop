/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeDebugInfoTest {
    @Test
    fun detectsJbrFromVendorVersion() {
        val info = captureRuntimeDebugInfo(
            props = mapOf(
                "java.vendor" to "JetBrains s.r.o.",
                "java.vendor.version" to "JBR-25.0.4.1+1-583.48-nomod",
                "java.version" to "25.0.4",
                "java.vm.name" to "OpenJDK 64-Bit Server VM",
                "java.home" to "/home/user/.jdks/jbr-25.0.4.1",
                "os.name" to "Linux",
                "os.arch" to "amd64",
                "os.version" to "6.18.46",
            )::get,
            env = mapOf("XDG_SESSION_TYPE" to "wayland", "WAYLAND_DISPLAY" to "wayland-1")::get,
            vmArgs = listOf("-Xmx2g", "-XX:+UseG1GC", "-Dfile.encoding=UTF-8"),
            maxHeapBytes = 2_000_000_000,
            pid = 42,
            cpus = 16,
        )
        assertTrue(info.isJbr)
        assertEquals("JBR 25.0.4", info.jvmLabel)
        assertEquals("Wayland", info.sessionType)
        assertTrue(info.copySummary().contains("JBR: yes"))
        assertTrue(info.copySummary().contains("-Xmx2g"))
        assertFalse(info.copySummary().contains("-Dfile.encoding"))
    }

    @Test
    fun detectsCorrettoAsNotJbr() {
        val info = captureRuntimeDebugInfo(
            props = mapOf(
                "java.vendor" to "Amazon.com Inc.",
                "java.vendor.version" to "Corretto-25.0.4.8.1",
                "java.version" to "25.0.4",
                "java.vm.name" to "OpenJDK 64-Bit Server VM",
                "java.home" to "/home/user/.jdks/corretto-25.0.4.1",
                "os.name" to "Linux",
                "os.arch" to "amd64",
                "os.version" to "6.18.46",
            )::get,
            env = mapOf("XDG_SESSION_TYPE" to "x11", "DISPLAY" to ":0")::get,
            vmArgs = emptyList(),
            maxHeapBytes = 512_000_000,
            pid = 7,
            cpus = 8,
        )
        assertFalse(info.isJbr)
        assertEquals("Corretto 25.0.4", info.jvmLabel)
        assertEquals("X11", info.sessionType)
        assertTrue(info.copySummary().contains("JBR: no"))
    }

    @Test
    fun nixOpenJdkWithNaVendorIsOpenJdk() {
        val info = captureRuntimeDebugInfo(
            props = mapOf(
                "java.vendor" to "N/A",
                "java.vendor.version" to "(nix)",
                "java.version" to "25.0.4",
                "java.vm.name" to "OpenJDK 64-Bit Server VM",
                "java.home" to "/nix/store/bpqms7zjncnn4w66bfhn91mxvynzlgsm-openjdk-25.0.4+7/lib/openjdk",
                "os.name" to "Linux",
                "os.arch" to "amd64",
                "os.version" to "6.18.46",
            )::get,
            env = mapOf("XDG_SESSION_TYPE" to "wayland", "WAYLAND_DISPLAY" to "wayland-1")::get,
            vmArgs = emptyList(),
            maxHeapBytes = 4_030_700_000,
            pid = 1,
            cpus = 8,
        )
        assertFalse(info.isJbr)
        assertEquals("OpenJDK 25.0.4 (Nix)", info.jvmLabel)
        assertEquals("Nix OpenJDK", info.vendor)
    }
}
