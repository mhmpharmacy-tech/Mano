# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep WebView JS bridge methods intact for 3D scene interactions
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve kotlinx.serialization metadata, serializers, and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }

# Keep Firestore DTOs and domain models used via reflection / serialization
-keep class com.example.data.*Cloud { *; }
-keep class com.example.domain.model.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Keep Firebase App Check providers and Firebase AI runtime types
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.firebase.ai.** { *; }
