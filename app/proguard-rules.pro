# LifeOS — ProGuard / R8 rules
# Keep Room entity classes (field names used via reflection by Room).
-keep class com.lifeos.data.db.entity.** { *; }

# Keep kotlinx.serialization generated serializers (used by type converters + export).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.lifeos.**$$serializer { *; }
-keepclassmembers class com.lifeos.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.lifeos.** { *; }

# Strip verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep biometric classes.
-keep class androidx.biometric.** { *; }

# Navigation Compose route classes.
-keepnames class com.lifeos.ui.navigation.** { *; }
