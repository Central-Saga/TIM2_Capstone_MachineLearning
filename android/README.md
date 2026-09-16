# KitchenGuard CSM — Android Studio Project
**Track 2: Automated Quality Control & Waste Prevention**  
**Mitra Industri:** PT Central Saga Mandala  
**Institusi:** Capstone Project ITB STIKOM Bali  

---

## 📱 Tema Tampilan
Aplikasi Android ini didesain dengan tema **Material Design 3 Light**:
- **Warna Dominan:** Putih Bersih (`#FFFFFF` dan `#F8FAFC`) untuk latar belakang dan kartu.
- **Warna Aksen:** Oranye Energik KitchenGuard (`#FF6B00` dan `#EA580C`) untuk tombol utama, tab aktif, bracket scanner laser, dan highlight confidence.
- **Teks:** Slate Gelap Kontras Tinggi (`#0F172A`) untuk kenyamanan membaca di lingkungan dapur.

---

## 🚀 Cara Membuka & Menjalankan di Android Studio

1. **Buka Android Studio:**
   - Pilih menu **File** -> **Open...**
   - Arahkan ke folder: `c:\CAPSTONE_MACHINE_LEARNING\android`
   - Klik **OK**.
2. **Sinkronisasi Gradle:**
   - Android Studio akan otomatis mendeteksi proyek Gradle (`KitchenGuardCSM`).
   - Tunggu hingga proses **Gradle Sync** selesai.
3. **Jalankan Backend ML (FastAPI):**
   - Pastikan backend Python berjalan di PC:
     ```powershell
     cd c:\CAPSTONE_MACHINE_LEARNING
     python app.py
     ```
   - Server akan aktif di `http://127.0.0.1:8000`.
   - Android Emulator otomatis terhubung ke host melalui alamat `http://10.0.2.2:8000/`.
4. **Pilih Emulator / Device Android:**
   - Pilih perangkat virtual (misal Pixel 7 / Pixel 8 dengan API 30+) atau sambungkan smartphone Android dengan USB Debugging.
5. **Klik Run (▶):**
   - Aplikasi akan dikompilasi dan dipasang di perangkat Android Anda.

---

## 🔍 Fitur-Fitur Utama

1. **Scan Bahan (Vision AI):**
   - Memindai citra bahan mentah: **Daging Sapi**, **Daging Ayam**, **Apel**, **Pisang**, **Tomat**, **Wortel**, **Paprika**, **Ikan Salmon**.
   - Menghasilkan status freshness: `FRESH`, `ACCEPTABLE`, `SPOILED`, `REJECT` dengan ambang batas PRD 85% Confidence Gate.
2. **Scan Barcode (Google ML Kit Barcode Scanning):**
   - Memindai kode batang kemasan EAN-13, EAN-8, Code-128.
   - Otomatis mencocokkan ke database inventori dapur (Nama Bahan, Batch ID, Supplier, Expired Date, dan Suhu Chiller).
3. **OCR Timbangan Digital (Google ML Kit Text Recognition):**
   - Membaca display angka berat timbangan digital (misal `1.45 kg`).
4. **Form Log Waste Terpadu:**
   - Menggabungkan data Barcode, Timbangan, Freshness, dan Alasan Limbah untuk disimpan ke database Android.
