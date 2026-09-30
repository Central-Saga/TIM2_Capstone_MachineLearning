import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ── Kredensial signing rilis (Issue #17) ──────────────────────────────────────
// Dibaca dari local.properties (tidak di-commit) atau environment variable.
// KEAMANAN: rilis TIDAK PERNAH ditandatangani dengan debug key.
// Bila kredensial tidak tersedia, APK release dibiarkan UNSIGNED + peringatan.
val releaseKeystoreProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

fun signingValue(key: String, envName: String): String? =
    (releaseKeystoreProperties.getProperty(key) ?: System.getenv(envName))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("RELEASE_STORE_FILE", "RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD")

val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { it != null }

android {
    namespace = "com.csm.kitchenguard"
    compileSdk = 37

    // ── Signing configs ──────────────────────────────────────────────────────
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.csm.kitchenguard"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BASE_URL configured per build type in buildTypes section
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"") // Debug - localhost emulator
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            
            // HTTP for local development server (localhost/emulator)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            
            // Network security config allows cleartext for localhost only
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // ── Signing rilis (Issue #17) ─────────────────────────────────────
            // JANGAN PERNAH memakai debug key untuk release.
            // - Bila kredensial keystore produksi tersedia → pakai signingConfig release.
            // - Bila tidak tersedia → APK release dibiarkan UNSIGNED (bukan debug-signed)
            //   beserta peringatan. Tidak dapat didistribusikan, tapi juga tidak
            //   menyamar sebagai rilis yang valid.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "⚠️  RELEASE_SIGNING tidak dikonfigurasi. APK 'release' akan UNSIGNED " +
                        "dan TIDAK dapat didistribusikan. Tambahkan RELEASE_STORE_FILE, " +
                        "RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD " +
                        "ke local.properties atau environment variable. " +
                        "Rilis TIDAK ditandatangani dengan debug key (Issue #17)."
                )
            }

            // HTTPS production API
            buildConfigField("String", "BASE_URL", "\"https://api.kitchenguard.com/\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Test JVM Configuration
    testOptions {
        // Android framework stubs (mis. android.util.Log) mengembalikan nilai
        // default alih-alih melempar "Method not mocked" pada unit test JVM.
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.jvmArgs(
                "-Dnet.bytebuddy.experimental=true",
                "-XX:+EnableDynamicAgentLoading",
                "-Xshare:off"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Room (Local Database / Offline-First)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (Non-Sensitive Session Preferences)
    implementation(libs.androidx.datastore.preferences)

    // Security & Encryption (EncryptedSharedPreferences untuk JWT Token)
    implementation(libs.android.security)

    // Networking (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // WorkManager (Sync Queue & Background Processing)
    implementation(libs.androidx.work.runtime.ktx)

    // ML Kit (OCR Digital Scale)
    implementation(libs.mlkit.text.recognition)

    // TensorFlow Lite (On-Device Freshness Classifier)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
    }

    // CameraX (Evidence Capture & OCR Camera)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Image Loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
