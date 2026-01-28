############################################
# GENERAL
############################################

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-dontoptimize
-dontpreverify

############################################
# ANDROID CORE
############################################

# Keep Activity, Service, Receiver, Provider
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep Application class
-keep class * extends android.app.Application

############################################
# VIEW / XML
############################################

-keepclassmembers class * {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

############################################
# RESOURCE / R CLASS
############################################

# Jangan hapus R class
-keep class **.R
-keep class **.R$*

############################################
# SERIALIZATION / REFLECTION
############################################

# Kalau pakai Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

############################################
# LOGGING
############################################

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

############################################
# WARNING HANDLING
############################################

-dontwarn **
