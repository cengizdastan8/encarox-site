# Kodo ProGuard rules

# Google API client / Gmail uses reflection over model classes
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.gmail.** { *; }
-keepclassmembers class * { @com.google.api.client.util.Key <fields>; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.naming.**

# Keep Room entities/DAOs metadata
-keep class com.encarox.kodo.data.db.** { *; }
