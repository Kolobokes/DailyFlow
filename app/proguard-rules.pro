# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes
-keep class com.dailyflow.app.data.model.** { *; }

# Keep Room entities
-keep class com.dailyflow.app.data.entity.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep SQLCipher classes
-keep class net.sqlcipher.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep kotlinx-datetime classes
-keep class kotlinx.datetime.** { *; }
-keep class kotlinx.datetime.serializers.** { *; }

# Suppress warnings for kotlinx-serialization (used internally by kotlinx-datetime)
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.serialization.internal.**
-dontwarn kotlinx.serialization.descriptors.**
-dontwarn kotlinx.serialization.encoding.**

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
