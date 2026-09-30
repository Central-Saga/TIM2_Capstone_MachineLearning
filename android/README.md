# KitchenGuard CSM 🧑‍🍳📊

KitchenGuard CSM adalah sistem manajemen dapur (*Kitchen Waste & Stock Control*) yang terdiri dari dua subproyek terpisah dalam satu repositori ini.

---

## 📁 Struktur Repositori

```
Capstone Kitchen Guard/
├── android-app/                    ← 📱 Aplikasi Android (Kotlin + Jetpack Compose)
│   ├── app/                        #   Source code utama Android
│   ├── gradle/, gradlew, gradlew.bat
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── Tim2_Capstone_Website_Admin/    ← 🌐 Web Admin Dashboard
│   └── folder_dev/                 #   Source code frontend web
│
├── KitchenGuard_prd.md             ← 📄 Product Requirements Document
├── Alur_Pembuatan_Aplikasi_Android_Kotlin.png
└── README.md                       ← Kamu sedang di sini
```

### Cara Membuka Subproyek

| Subproyek | Cara Membuka |
|---|---|
| **Android App** | Buka folder `android/` di **Android Studio** |

---

## 📱 Android App — KitchenGuard CSM

KitchenGuard CSM adalah aplikasi Android *offline-first* kelas *enterprise* yang dirancang khusus untuk memantau, mencatat, dan menganalisis **Kitchen Waste & Stock Control** di ekosistem restoran. Aplikasi ini memastikan pencatatan sisa bahan (waste) dan perhitungan inventaris dapat berjalan tanpa hambatan, bahkan saat kondisi dapur tidak memiliki sinyal internet (Offline-First).

---

## 🎯 Fitur Utama

1. **Offline-First Architecture (Room DB + WorkManager)**
   - Semua data akan disimpan di memori lokal (Room Database) seketika saat staf menekan tombol "Simpan".
   - Sinkronisasi latar belakang berjalan secara reaktif: **Immediate Sync** saat internet aktif, dan **Periodic Sync** setiap 15 menit sebagai cadangan (*safety net*).
   
2. **CameraX & OCR Timbangan (Google ML Kit)**
   - Kasir / Staf tidak perlu mengetikkan angka berat secara manual. Aplikasi menggunakan pemindai kamera (OCR) untuk membaca angka di layar timbangan digital secara otomatis.
   - Deteksi *Confidence Score* internal membantu menolak bacaan yang buram atau kurang meyakinkan.

3. **On-Device AI Freshness Classification (TensorFlow Lite)**
   - Sistem klasifikasi Machine Learning bawaan (`.tflite`) yang menganalisis kesegaran bahan baku (*Fresh, Acceptable, Spoiled, Reject*).
   - Memiliki alur konfirmasi **Human-in-the-Loop**: jika AI meragukan kesegaran suatu produk (*confidence* < 85%), staf dapur diberi kuasa untuk merevisi dan memverifikasi penilaian secara manual sebelum data disimpan.

   > ⚠️ **Status Model — Belum Tersedia**
   > File model `freshness-v1.tflite` **belum disertakan di repositori ini** dan masih menunggu penyediaan dari tim ML.
   >
   > Aplikasi **tidak** memakai hasil acak/mock sebagai pengganti. Bila model tidak ada, layar AI Verification menampilkan status jujur **"AI Tidak Tersedia — Menunggu Observasi Manual"** dan meminta staf melakukan verifikasi visual sesuai SOP (lihat `app/src/main/assets/README.md`).
   >
   > Untuk mengaktifkan AI: letakkan `freshness-v1.tflite` di `app/src/main/assets/`, lalu rebuild. Tidak ada perubahan kode yang diperlukan.

4. **Modern UI/UX (Jetpack Compose)**
   - Dibangun 100% menggunakan arsitektur *Declarative UI* Jetpack Compose.
   - Menerapkan *StateFlow* dan *MVI (Model-View-Intent)* untuk pengelolaan _state_ yang bersih dan mulus (seperti transisi loading dan *real-time monitoring* kerugian HPP (Harga Pokok Penjualan) bahan makanan).

---

## 🛠️ Stack Teknologi

- **Bahasa**: Kotlin (Target JVM 17)
- **UI Framework**: Jetpack Compose (Material 3)
- **Arsitektur**: Clean Architecture (Domain, Data, Presentation) + MVI
- **Database & Lokal**: Room Database, Jetpack DataStore
- **Networking**: Retrofit2, OkHttp (dengan *Auth Interceptors*)
- **Background Jobs**: WorkManager (CoroutineWorker)
- **Perangkat Keras & AI**: CameraX, Google ML Kit (Text Recognition), TensorFlow Lite (TFLite)
- **Dependency Injection**: Manual Factory (Siap untuk migrasi ke Dagger Hilt)

---

## 🚀 Instalasi & Kompilasi Lokal

Proyek ini dibangun menggunakan **Android Studio** terbaru (AGP 9.3.2, compileSdk 37).

1. Lakukan _clone_ repositori:
   ```bash
   git clone https://github.com/Central-Saga/TIM_2_ANDROID.git
   ```
2. Buka proyek menggunakan Android Studio.
3. Sinkronisasikan Gradle (`Sync Project with Gradle Files`).
4. Jalankan aplikasi ke Emulator Android atau perangkat fisik dengan menekan **Run (Shift + F10)**.

> **Opsional — Mengaktifkan AI Freshness:**
> Fitur AI kesegaran memerlukan file model yang belum disertakan di repo ini.
> Letakkan `freshness-v1.tflite` di `app/src/main/assets/` sebelum build.
> Tanpa file ini, aplikasi tetap berjalan normal dan menampilkan status
> *"AI Tidak Tersedia"* secara jujur (tanpa hasil acak).
> Detail spesifikasi model: lihat `app/src/main/assets/README.md`.

### Menjalankan Test & Lint secara Lokal

Dari folder `android-app/`:

```bash
./gradlew testDebugUnitTest   # unit test (JVM)
./gradlew lintDebug            # Android Lint
./gradlew assembleDebug        # build APK debug
```

Perintah yang sama dijalankan otomatis oleh **GitHub Actions** pada setiap
push/PR (lihat `.github/workflows/android-ci.yml`).

---

## 📂 Struktur Proyek

```text
app/src/main/
├── assets/
│   └── README.md      # Spesifikasi model AI & cara menaruh freshness-v1.tflite
└── java/com/csm/kitchenguard/
    ├── data/              # Implementasi Repositori, Room DAOs, Retrofit API, WorkManager, DataStore
    ├── di/                # Dependency Injection container manual (AppModule)
    ├── domain/            # Use Cases (Aturan Bisnis Inti) dan Abstraksi Repositori
    ├── presentation/      # Jetpack Compose Screens, ViewModels (MVI), dan Navigasi (NavHost)
    ├── utils/             # Helper untuk TensorFlow Lite Classifier & Model Data AI
    └── MainActivity.kt    # Entry Point UI & Penjadwalan WorkManager
```

---

## 📌 Status Fase Pengembangan (Roadmap)

- [x] **FASE 1**: Infrastruktur Proyek, Tema Compose, Navigasi, dan **Penyelesaian Seluruh UI/UX (Figma M01 - M13)**
  - *M01: Login (Secure Portal)*
  - *M02: Reset Password*
  - *M03: Kitchen Station Hub (Beranda)*
  - *M04: Waste Logging (Pencatatan Limbah)*
  - *M05: AI Verification (Kamera TFLite & ML Kit)*
  - *M06: Stock Audit (Anomali Selisih)*
  - *M07: Shift Summary (Rekapitulasi)*
  - *M08: Notification Center (Pusat Peringatan)*
  - *M09: Profile Screen (Statistik Pekerja)*
  - *M10: Edit Profile (Otoritas Dapur)*
  - *M11: Change Photo (Modal Bottom Sheet)*
  - *M12: Change Password (Indikator Kekuatan Sandi)*
  - *M13: Settings (Preferensi Aplikasi & Workflow)*
- [x] **FASE 2**: Room Database, DAO, Local Preferences (DataStore)
- [x] **FASE 3**: Retrofit API Client, Clean Architecture, WorkManager Background Sync
- [x] **FASE 4**: Integrasi Hardware (CameraX OCR Timbangan) & Machine Learning (TFLite Freshness AI)
  - *Pipeline TFLite on-device, confidence gate 85%, & integrasi UI selesai.*
  - ⚠️ *File model `freshness-v1.tflite` masih menunggu tim ML — lihat catatan di atas.*
- [x] **FASE 5**: Continuous Integration / Pengujian Unit
  - *Unit test: Use Case, OCR confidence gate, AI Freshness gate, sync mapping,
    enqueue, conflict retry, notification cache (`./gradlew testDebugUnitTest`).*
  - *CI: GitHub Actions (`.github/workflows/android-ci.yml`) menjalankan
    unit test + lint + `assembleDebug` pada setiap push/PR ke `android-app/`.*
