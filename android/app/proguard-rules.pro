# Keep DTOs & Gson Serialization
-keepclassmembers class com.csm.kitchenguard.data.remote.dto.** { *; }
-keep class com.csm.kitchenguard.data.remote.dto.** { *; }

# Keep Room Database Models
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }

# Keep TensorFlow Lite & ML Kit
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
