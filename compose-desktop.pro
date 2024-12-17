
# Pakku
-keep class pakkupro.** { *; }

# Compose
-keep class org.jetbrains.jewel.** { *; }

# Warnings
-dontwarn com.github.ajalt.mordant.internal.**
-dontwarn okhttp3.internal.**
-dontwarn com.sun.jna.internal.**
-dontwarn kotlin.collections.builders.**
-dontwarn kotlin.jdk7.**
-dontwarn kotlinx.serialization.json.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.jewel.**

# FileKit
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

############ default rules start ###############

-keep class kotlin.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Kotlinx Coroutines Rules
# https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal
-dontwarn java.lang.ClassValue

# https://github.com/JetBrains/compose-jb/issues/2393
-dontnote kotlin.coroutines.jvm.internal.**
-dontnote kotlin.internal.**
-dontnote kotlin.jvm.internal.**
-dontnote kotlin.reflect.**
-dontnote kotlinx.coroutines.debug.internal.**
-dontnote kotlinx.coroutines.internal.**

# this is a weird one, but breaks build on some combinations of OS and JDK (reproduced on Windows 10 + Corretto 16)
-dontwarn org.graalvm.compiler.core.aarch64.AArch64NodeMatchRules_MatchStatementSet*

########### default rules end ##################

-keep class org.apache.** { *; }
-keep class org.jetbrains.** { *; }
-keep class org.sqlite.** { *; }
-keep class org.orbitmvi.** { *; }
