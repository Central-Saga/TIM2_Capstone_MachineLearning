# Proguard rules for KitchenGuard CSM Android App
# Keeps TFLite, GSON, Retrofit, CameraX and Models

-keep class org.tensorflow.lite.** { *; }
-keep class com.kitchenguard.csm.model.** { *; }
-keep class com.kitchenguard.csm.utils.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
