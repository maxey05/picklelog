# --- Room ------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- kotlinx.serialization -------------------------------------------------
# The compiler plugin generates a $$serializer for each @Serializable class and
# reaches it reflectively through the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# Tightened at RFC-002 (AC-1.10 deferred this until real @Serializable classes existed).
# com.maxeydev.picklelog.data.profile is the only package with any, so the global
# wildcards above are narrowed to it. Widen this if another package gains @Serializable.
-keepclassmembers class com.maxeydev.picklelog.data.profile.**$$serializer { *; }
-keepclasseswithmembers class com.maxeydev.picklelog.data.profile.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.maxeydev.picklelog.data.profile.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# --- Play Billing ----------------------------------------------------------
# Billing responses are parsed from JSON into library types; keep the surface.
-keep class com.android.billingclient.api.** { *; }

# --- R93 — logging is stripped in release ----------------------------------
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
