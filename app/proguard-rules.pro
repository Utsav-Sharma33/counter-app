# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
# More information about the purpose of ProGuard, see the introduction in this file.

# -verbose

# Optimization options must be turned off for app bundles.
# -optimizationpasses 5
# -dontobfuscate
# ...

# If you want to enable optimization, change usages of this "proguard-android.txt"
# to "proguard-android-optimize.txt"

# For libraries, change usages of "proguard-android.txt" to either
#  "proguard-android-optimize.txt" or your own specific keep file(s).

# Get the default ProGuard rules for apps. See below for an explanation of what you
# can change here.

# Keep classes and methods that have the @Keep annotation
-keepclasseswithmembers class * {
    @androidx.annotation.Keep *;
}

# Room database
-keep class com.example.counter.data.** { *; }

# Coroutines
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
