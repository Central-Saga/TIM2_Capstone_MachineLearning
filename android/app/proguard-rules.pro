# ── Keep DTOs, Domain Models & Local Entities ────────────────────────────────
-keepclassmembers class com.csm.kitchenguard.data.remote.dto.** { *; }
-keep class com.csm.kitchenguard.data.remote.dto.** { *; }
-keep class com.csm.kitchenguard.domain.model.** { *; }
-keep class com.csm.kitchenguard.data.local.entity.** { *; }

# ── Gson Rules ───────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# ── Retrofit 2 ───────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.csm.kitchenguard.data.remote.api.** { *; }

# ── OkHttp 3 ─────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Keep Room Database Models & DAOs ─────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Keep TensorFlow Lite & ML Kit ────────────────────────────────────────────
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
