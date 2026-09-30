# PRODUCT REQUIREMENTS DOCUMENT (PRD)

## KitchenGuard CSM — Android Mobile App

**Dokumen Turunan dari:** KitchenGuard CSM PRD v1.3 — Baseline Implementasi  
**Versi:** Android v1.1 (Updated with Backend & Infrastructure Integration Stack)  
**Tanggal:** 16 September 2026  
**Mitra Industri:** PT Central Saga Mandala  
**Program:** Capstone Project ITB STIKOM Bali — Track 2: Automated Quality Control & Waste Prevention

---

# 1\. Ringkasan Produk

**KitchenGuard CSM Android** adalah aplikasi mobile offline-first untuk Kitchen Staff, Barista, dan Inventory Checker.

Fokus utama aplikasi:

- waste logging;  
- OCR timbangan digital;  
- AI freshness on-device;  
- physical stock audit;  
- batch/expiry context;  
- offline transaction queue;  
- sync aman menggunakan idempotency key;  
- shift context;  
- rekap shift.

Android berfungsi sebagai **operational client**, sedangkan stock calculation, financial calculation, approval, dan variance final tetap **server-authoritative** yang dikelola oleh backend dengan persistensi data di **PostgreSQL**, manajemen konkurensi/antrean sinkronisasi di **Redis**, dan seluruh layanan server diorkestrasi menggunakan **Podman**.

---

# 2\. Tujuan Android

1. Membuat waste logging cepat dan sederhana di lingkungan dapur operasional.  
2. Memungkinkan operasional pencatatan tetap berjalan lancar saat koneksi internet terputus (offline-first).  
3. Mencegah duplicate transaction saat koneksi pulih dan aplikasi melakukan retry.  
4. Membaca berat timbangan secara otomatis dengan OCR.  
5. Menjalankan freshness classification secara mandiri di perangkat (on-device AI).  
6. Melakukan physical stock audit di akhir shift.  
7. Menjaga setiap transaksi tetap terikat secara valid ke shift aktif.  
8. Memberikan feedback status sinkronisasi, retry, dan resolusi konflik yang transparan.

---

# 3\. Pengguna

## 3.1 Kitchen Staff

- Melihat status active shift.  
- Mencatat transaksi waste bahan mentah.  
- Menggunakan OCR untuk menangkap bobot timbangan digital.  
- Mengambil foto evidence kondisi bahan.  
- Menjalankan on-device AI freshness scan.  
- Memantau antrean sinkronisasi (sync queue).

## 3.2 Barista

Workflow serupa dengan Kitchen Staff yang disesuaikan untuk station beverage/bar.

## 3.3 Inventory Checker

- Melakukan physical stock count saat tutup shift.  
- Memeriksa batch code dan tanggal kedaluwarsa bahan.  
- Memasukkan data audit fisik untuk rekonsiliasi stok di server.

---

# 4\. Scope MVP Android

## P0 — Required

1. Authentication (REST API \+ Token-based).  
2. Cached authenticated session di penyimpanan lokal terenkripsi.  
3. Active shift context management.  
4. Kitchen Station Hub.  
5. Waste Logging flow lengkap.  
6. Ingredient & batch selection.  
7. Unit conversion input display.  
8. OCR weight capture via kamera.  
9. Basic TFLite freshness classification on-device.  
10. Room / SQLite local persistence untuk penyimpanan offline-first.  
11. Local sync queue dengan WorkManager.  
12. Idempotency key generation per transaksi.  
13. Physical stock audit input.  
14. Conflict & shift-lock error handling.  
15. Basic shift summary.

## P1

- Notification Center.  
- Profile management.  
- Profile photo upload.  
- Advanced batch/expiry UX.  
- PDF/QR viewing.  
- Offline diagnostics.

## P2

- Biometric authentication (Fingerprint).  
- Advanced app settings.  
- Integrasi periferal timbangan via Bluetooth/Serial.

---

# 5\. Non-Goals

Aplikasi Android bukan:

- Source of truth untuk stok atau financial loss.  
- Point of Sale (POS) application.  
- Admin dashboard manajerial (dikelola melalui Web Admin).  
- Portal manajemen supplier / purchasing.  
- Modul akuntansi / ERP.

---

# 6\. Technology Stack

Arsitektur sistem membagi tanggung jawab stack antara **Client (Mobile App)** dan **Backend / Infrastructure (Server-Side Integration Contract)**:

### 6.1 Mobile Client Stack (Android)

| Komponen / Layer | Teknologi | Keterangan |
| :---- | :---- | :---- |
| **Bahasa Pemrograman** | Kotlin | Bahasa utama pengembangan native Android. |
| **UI Framework** | Jetpack Compose | Deklaratif UI modern dan reaktif. |
| **Local Database** | Room (SQLite) | Penyimpanan lokal offline-first (draft, cache master data, antrean sync). |
| **Background Processing** | WorkManager | Penjadwalan sinkronisasi background saat konektivitas tersedia. |
| **Networking / REST API** | Retrofit 2 \+ OkHttp 3 | Komunikasi HTTP dengan backend API. |
| **OCR Engine** | Google ML Kit (Text Recognition) | Ekstraksi angka berat dari layar timbangan digital secara on-device. |
| **On-Device Machine Learning** | TensorFlow Lite (TFLite) | Inferensi klasifikasi kesegaran bahan visual (Fresh, Acceptable, Spoiled, Reject). |
| **Image Loading / Caching** | Coil | Rendering foto evidence dan preview bahan. |

### 6.2 Backend & Infrastructure Integration Stack (Server Context)

Aplikasi Android berinteraksi dengan infrastruktur backend yang memenuhi spesifikasi berikut:

| Komponen / Layer | Teknologi | Peran & Integrasi dengan Android |
| :---- | :---- | :---- |
| **Database Server (Source of Truth)** | PostgreSQL | Menyimpan master bahan, batch, log transaksi waste, audit fisik, dan ledger stok. Sumber validasi utama data sync. |
| **Cache, Lock & Queue Engine** | Redis | 1\. **Idempotency Store:** Memvalidasi `idempotency_key` dari transaksi Android agar retry tidak menghasilkan duplikasi. 2\. **Session / Active Shift Cache:** Validasi cepat status shift aktif. 3\. **Sync Queue:** Buffer pemrosesan background batch sync dari client. |
| **Containerization & Runtime** | Podman | Mengisolasi dan menjalankan multi-container environment (Backend API service, PostgreSQL database, dan Redis instance) secara rootless dan konsisten. |
| **Protokol Komunikasi** | RESTful JSON API | Format pertukaran data push/pull sync melalui HTTPS dengan otentikasi Bearer token. |

---

# 7\. Design System

70% White / Light Neutral

30% Orange Brand Accent

Palette warna:

- **Primary Orange:** `#F97316`  
- **Strong Orange:** `#EA580C`  
- **White / Surface:** `#FFFFFF`  
- **Background:** `#F8FAFC`  
- **Text Primary:** `#1F2937`  
- **Success:** `#16A34A`  
- **Warning:** `#D97706`  
- **Danger:** `#DC2626`

Typography:

- **Inter:** Teks umum, label, form, dan deskripsi.  
- **JetBrains Mono:** Menampilkan angka bobot timbangan, nilai Rupiah, token teknis, dan idempotency key.

Prinsip UX:

- Satu layar \= satu tujuan operasional.  
- Minimum typing, prioritaskan seleksi tap dan OCR scan.  
- CTA besar dan kontras tinggi untuk lingkungan dapur yang sibuk.  
- Touch target minimum 48dp.

---

# 8\. Navigasi

Bottom navigation utama:

1. **Dapur (Station Hub):** Status shift, ringkasan aktivitas, tombol cepat input.  
2. **Timbang & Scan:** Akses langsung kamera untuk OCR timbangan dan AI freshness scan.  
3. **Rekap Shift:** Rekapitulasi transaksi shift berjalan dan tombol tutup shift/audit fisik.

Menu pelengkap:

- Notification Center.  
- Profile & Pengaturan Stasiun.

---

# 9\. Authentication

Endpoint:

POST /api/v1/auth/login

Input:

- `identifier` (Email atau Employee ID)  
- `password`

Aturan Client:

- Menggunakan Bearer Token (JWT) yang disimpan di EncryptedSharedPreferences.  
- Mendukung cached session offline: staff yang sudah login dapat tetap membuka aplikasi saat jaringan terputus selama token belum kedaluwarsa.  
- Tidak menyediakan registrasi publik (akun dibuat melalui Web Admin).

---

# 10\. Active Shift Context

Data shift disimpan di Room local DB:

shift\_id

station\_id

started\_at

server\_version

Aturan:

- Shift dikontrol dan ditentukan oleh server (PostgreSQL/Redis).  
- Client mengikat setiap transaksi waste ke `shift_id` aktif saat transaksi dibuat.  
- Waktu perangkat (device clock) tidak boleh memindahkan transaksi ke shift lain secara otomatis.

---

# 11\. Kitchen Station Hub

Menampilkan:

- Indikator Active Shift & Nama Station.  
- Total Waste hari ini & estimasi loss (display-only).  
- Status antrean sinkronisasi (Pending Sync Counter).  
- Riwayat aktivitas transaksi terkini pada shift aktif.

Aksi Utama:

- **Catat Waste Baru** (membuka alur pencatatan).  
- **Audit Fisik Tutup Shift** (membuka alur penghitungan fisik akhir shift).

---

# 12\. Waste Logging Flow

Kitchen Hub

&nbsp;&nbsp;&nbsp;↓

Catat Waste

&nbsp;&nbsp;&nbsp;↓

OCR Weight / Manual Input

&nbsp;&nbsp;&nbsp;↓

Konfirmasi Berat (Confirm Weight)

&nbsp;&nbsp;&nbsp;↓

Pilih Bahan (Ingredient) & Batch

&nbsp;&nbsp;&nbsp;↓

Pilih Alasan Waste (Reason)

&nbsp;&nbsp;&nbsp;↓

AI Freshness Scan (Opsional/Sesuai SOP)

&nbsp;&nbsp;&nbsp;↓

Simpan ke Local Room DB (Immediate Persistence)

&nbsp;&nbsp;&nbsp;↓

Daftarkan ke Sync Queue (WorkManager)

Data Payload Minimum:

{

&nbsp;&nbsp;"client\_uuid": "c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",

&nbsp;&nbsp;"idempotency\_key": "waste:c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",

&nbsp;&nbsp;"shift\_id": 14,

&nbsp;&nbsp;"station\_id": 2,

&nbsp;&nbsp;"ingredient\_id": 105,

&nbsp;&nbsp;"batch\_id": 88,

&nbsp;&nbsp;"quantity": 1.45,

&nbsp;&nbsp;"unit": "kg",

&nbsp;&nbsp;"reason": "SPOILED",

&nbsp;&nbsp;"client\_event\_at": "2026-09-16T13:30:00Z",

&nbsp;&nbsp;"ai\_metadata": {

&nbsp;&nbsp;&nbsp;&nbsp;"predicted\_class": "SPOILED",

&nbsp;&nbsp;&nbsp;&nbsp;"confidence": 0.94,

&nbsp;&nbsp;&nbsp;&nbsp;"model\_version": "freshness-v1.0"

&nbsp;&nbsp;},

&nbsp;&nbsp;"ocr\_metadata": {

&nbsp;&nbsp;&nbsp;&nbsp;"ocr\_raw\_text": "1.450 kg",

&nbsp;&nbsp;&nbsp;&nbsp;"ocr\_confidence": 0.96

&nbsp;&nbsp;}

}

---

# 13\. Unit Conversion

Unit Input yang didukung:

- Gram (`g`), Kilogram (`kg`)  
- Mililiter (`ml`), Liter (`liter`)  
- Pieces (`pcs`)

Aplikasi Android melakukan kalkulasi konversi hanya untuk keperluan visual/display. Nilai baku dasar (canonical units) dan kalkulasi loss finansial dihitung dan divalidasi oleh backend (PostgreSQL).

---

# 14\. Batch & Expiry Context

Untuk alasan waste `Basi / Expired`:

- Form mewajibkan pemilihan `batch_id` jika stok memiliki batch aktif.  
- Menampilkan status batch: `ACTIVE`, `NEAR_EXPIRY`, `EXPIRED`.  
- Nilai sisa kuantitas batch pada Android bersifat read-only cache dari PostgreSQL.

---

# 15\. OCR Digital Scale

Integrasi: **Google ML Kit Text Recognition**

Aturan Confidence:

- `Confidence >= 0.90`: Angka bobot langsung terisi otomatis pada form.  
- `Confidence < 0.90`: Aplikasi menandai nilai perlu diverifikasi manual oleh staff.  
- Pengguna wajib menekan tombol konfirmasi ("Confirm Weight") sebelum menyimpan. OCR tidak pernah melakukan auto-submit.

---

# 16\. On-Device AI Freshness Classification

Model: **TensorFlow Lite (`freshness-v1.tflite`)**

Kelas Output:

- `FRESH`, `ACCEPTABLE`, `SPOILED`, `REJECT`  
- Status Khusus: `UNCERTAIN`

Aturan Confidence Gate:

- `Confidence >= 85%`: Prediksi kelas ditampilkan sebagai pendukung keputusan (*decision support*).  
- `Confidence < 85%`: Output dialihkan menjadi `UNCERTAIN`. Staff diminta melakukan observasi visual manual. AI tidak boleh melakukan auto-reject secara mandiri.

---

# 17\. Evidence Capture

- Kamera aplikasi mengambil foto barang yang dibuang.  
- Kompresi gambar dilakukan di sisi client (format JPG, resolusi standar operasional).  
- Foto dikirimkan bersama payload sync atau melalui endpoint upload multipart terproteksi.

---

# 18\. Offline Local Database (Room / SQLite)

Struktur tabel lokal di SQLite/Room mencakup:

1. `cached_user`: Profil dan token sesi.  
2. `active_shift`: Metadata shift aktif saat ini.  
3. `ingredients`: Cache master bahan makanan.  
4. `batches`: Cache daftar batch aktif dan tanggal kedaluwarsa.  
5. `waste_records`: Data pencatatan waste lokal.  
6. `sync_queue`: Antrean transaksi yang siap dikirimkan ke server.  
7. `draft_stock_audit`: Draft penghitungan fisik akhir shift.  
8. `notifications_cache`: Cache riwayat notifikasi lokal.

---

# 19\. Sync State & Architecture

State transaksi lokal:

- `PENDING`: Tersimpan di Room, menunggu jadwal sync WorkManager.  
- `SYNCING`: Sedang dalam proses HTTP request ke API.  
- `SYNCED`: Dikonfirmasi sukses oleh server (tersimpan di PostgreSQL).  
- `FAILED`: Gagal jaringan, akan dijadwalkan ulang secara otomatis dengan exponential backoff.  
- `CONFLICT`: Ditolak oleh server (misal: shift telah ditutup/dikunci). Memerlukan tindakan atau review.

---

# 20\. Idempotency & Peran Redis di Backend

1. **Client Generation:** Android menghasilkan `client_uuid` (UUID v4) dan membuat `idempotency_key = waste:<client_uuid>`.  
2. **Persistence:** Kunci ini disimpan permanen di tabel `sync_queue` lokal. Jika transmisi gagal atau timeout, retry akan menggunakan `idempotency_key` yang sama.  
3. **Backend Validation via Redis:**  
   - Server backend KitchenGuard menggunakan **Redis** untuk memeriksa keberadaan `idempotency_key`.  
   - Jika kunci sedang diproses: request duplikat ditahan/ditolak (*concurrency lock*).  
   - Jika kunci sudah pernah diproses: server mengembalikan respon `ALREADY_PROCESSED` beserta ID data yang tersimpan di **PostgreSQL** tanpa membuat record ganda.  
   - Android memperlakukan `ALREADY_PROCESSED` sebagai status sukses (`SYNCED`).

---

# 21\. Push Sync Batch

Endpoint:

POST /api/v1/sync/batch

Dijalankan secara berkala oleh `WorkManager` dengan kriteria constraint: `NetworkType.CONNECTED`.

Status respon item yang diterima Android:

- `SYNCED`: Sukses disimpan di PostgreSQL.  
- `ALREADY_PROCESSED`: Sudah tercatat di backend (terverifikasi via Redis).  
- `CONFLICT`: Konflik status shift (HTTP 409).  
- `FAILED_VALIDATION`: Payload tidak valid (HTTP 422).

---

# 22\. Pull Sync (Pembaruan Master Data)

Endpoint:

POST /api/v1/sync/pull

Request:

{

&nbsp;&nbsp;"last\_sync\_cursor": "2026-09-16T10:00:00Z",

&nbsp;&nbsp;"entities": \["ingredients", "batches", "shifts", "notifications"\]

}

Android memperbarui database Room lokal berdasarkan data terbaru dari PostgreSQL server.

---

# 23\. Shift Lock Conflict Handling

Jika transaksi offline baru tersinkronisasi setelah shift di server dikunci (*Shift Locked*):

- Server mengembalikan kode `SYNC_CONFLICT_SHIFT_LOCKED` (HTTP 409).  
- Android menandai transaksi lokal dengan status `CONFLICT`.  
- Data lokal **tidak dihapus**.  
- UI menampilkan peringatan agar transaksi diverifikasi bersama Head Chef.

---

# 24\. Physical Stock Audit

Alur Audit Fisik:

1. Inventory Checker menginput kuantitas fisik riil (`actual_physical`) per bahan dan batch.  
2. Data disimpan sebagai draft di Room.  
3. Setelah dikonfirmasi, data dikirim ke endpoint `POST /api/v1/stock-audits`.  
4. Server (PostgreSQL) menghitung formula varian: $$\\text{Expected Closing} \= \\text{Opening} \+ \\text{Received} \- \\text{POS Consumption} \- \\text{Recorded Waste} \+ \\text{Adjustments}$$ $$\\text{Variance} \= \\text{Actual Physical} \- \\text{Expected Closing}$$  
5. Android menerima dan menampilkan status kalkulasi dari server.

---

# 25\. Deployment & Development Backend (Podman Context)

Untuk mendukung pengujian aplikasi Android di lingkungan lokal maupun server staging:

- Seluruh layanan backend dijalankan dalam kontainer **Podman**.  
- Konfigurasi pod / kontainer mencakup:  
  1. Kontainer **Web API Service** (REST API)  
  2. Kontainer **PostgreSQL Database** (Data Persistence)  
  3. Kontainer **Redis** (Key-Value Caching & Idempotency Store)  
- Pengembang Android menghubungkan `BASE_URL` aplikasi ke alamat IP host/kontainer Podman yang terekspos.

---

# 26\. Error Handling & HTTP Mapping

| Kode HTTP | Penanganan di Android |
| :---- | :---- |
| `200 / 201` | Transaksi berhasil, update status Room menjadi `SYNCED`. |
| `401` | Token kedaluwarsa, arahkan pengguna ke layar login atau refresh token. |
| `403` | Pengguna tidak memiliki hak akses pada stasiun terkait. |
| `409` | Konflik state (shift lock / version mismatch), tandai status `CONFLICT`. |
| `422` | Kesalahan validasi payload data. |
| `500 / 503` | Gangguan server backend, pertahankan antrean di `sync_queue` untuk retry otomatis. |

---

# 27\. Testing Strategy

1. **Unit & Dao Test (Android):**  
   - Pengujian Room DAO untuk operasi CRUD offline.  
   - Pengujian pembentukan `idempotency_key` dan state transition pada `sync_queue`.  
   - Pengujian confidence gate untuk OCR dan TFLite.  
2. **Integration Test (Android ke Podman Backend):**  
   - Pengujian pengiriman batch sync ke API backend yang berjalan di Podman.  
   - Simulasi koneksi terputus (airplane mode) lalu reconnect untuk memverifikasi anti-duplikasi via Redis.  
   - Pengujian pengiriman transaksi pada shift yang sudah berstatus locked di PostgreSQL.

---

# 28\. Definition of Done (DoD)

Fitur pada modul Android dinyatakan selesai jika:

1. UI telah diimplementasikan menggunakan Jetpack Compose sesuai design system.  
2. Operasi penyimpanan lokal offline-first menggunakan Room berjalan tanpa kendala.  
3. Mekanisme idempotency key terbukti mencegah duplikasi transaksi saat sinkronisasi ulang dengan backend (PostgreSQL \+ Redis).  
4. Model TFLite on-device inference dan Google ML Kit OCR terintegrasi dengan confidence threshold yang ditentukan.  
5. WorkManager sukses melakukan sinkronisasi otomatis saat perangkat terhubung kembali ke jaringan.  
6. Penanganan konflik (HTTP 409\) teruji tidak menghilangkan data lokal.  
7. Kode lolos tahap review dan memenuhi standar arsitektur native Android.

&nbsp;