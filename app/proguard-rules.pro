# CipherSpend Production ProGuard Rules
# Target: Android SDK 36 (Google Play Compliant)

# 1. SQLCipher for Android
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
-keep class androidx.sqlite.db.** { *; }
-dontwarn androidx.sqlite.db.**

# 2. TensorFlow Lite & Support Library
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# 3. Gson & TypeToken Serialization Preservation
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }

# 4. Room Database Entities, DAOs, & SQLite Architecture
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class com.example.cipherspend.data.** { *; }
-keepclassmembers class com.example.cipherspend.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# 5. Kotlin Coroutines & Reflection Meta
-keepattributes EnclosingMethod,InnerClasses
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
