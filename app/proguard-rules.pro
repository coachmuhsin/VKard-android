# ProGuard Rules for VKARD PRO Android

# Keep Kotlin Serialization and Reflection metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep serializable model classes and their synthetic serializing classes
-keep class com.vkard.pro.domain.model.** { *; }
-keepclassmembers class com.vkard.pro.domain.model.** {
    *** Companion;
    *** $serializer;
}

# Keep kotlinx.serialization classes
-keep class kotlinx.serialization.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }

# Ignore missing classes from compile-only annotations and system APIs
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.lang.management.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
