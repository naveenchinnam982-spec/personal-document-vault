# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve Room database entities, DAOs, and database classes
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomOpenHelper { *; }
-dontwarn androidx.room.paging.**

# Keep data models
-keep class com.example.data.model.** { *; }

# Keep Moshi / JSON Serialization
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep AndroidX Lifecycle / ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep Biometric prompt & security crypto classes
-keep class androidx.biometric.** { *; }
-keep class com.example.data.security.** { *; }

# Keep all project application code, entities, and UI state models
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve line numbers for readable crash logs
-keepattributes SourceFile,LineNumberTable

