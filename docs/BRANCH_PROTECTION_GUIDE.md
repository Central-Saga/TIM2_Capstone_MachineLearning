# 🛡️ Panduan Konfigurasi Branch Protection & Human Review
> **Repository**: `https://github.com/Central-Saga/TIM2_Capstone_MachineLearning`  
> **Target Branch**: `main`

Sesuai instruksi evaluasi dan standar engineering:
> **"Aktifkan branch protection + wajib review manusia sebelum merge ke main."**

Berikut adalah panduan langkah demi langkah untuk mengaktifkannya di GitHub:

---

## Langkah Konfigurasi di GitHub Web UI:

1. Buka repositori di browser:  
   👉 [https://github.com/Central-Saga/TIM2_Capstone_MachineLearning/settings/branches](https://github.com/Central-Saga/TIM2_Capstone_MachineLearning/settings/branches)
2. Pada bagian **Branch protection rules**, klik tombol **Add branch protection rule** (atau edit rule yang ada untuk branch `main`).
3. Pada kolom **Branch name pattern**, ketik:
   ```text
   main
   ```
4. Aktifkan opsi-opsi berikut:
   - ✅ **Require a pull request before merging**
     - ✅ **Require approvals**: Atur minimal `1` approval dari reviewer manusia.
     - ✅ **Dismiss stale pull request approvals when new commits are pushed** (opsional, disarankan).
   - ✅ **Require status checks to pass before merging**
     - Cari dan centang job CI: `Lint, Unit Test & ML Model Validation` (dari GitHub Actions `.github/workflows/ml-ci.yml`).
     - ✅ **Require branches to be up to date before merging**.
   - ✅ **Do not allow bypassing the above settings** (berlaku untuk admin dan kolaborator).
5. Klik tombol hijau **Save changes** di bagian bawah.

---

## Alur Kerja Git Standar Tim:
1. Pengembang bekerja di branch fitur atau branch `development`.
2. Setelah kode selesai dan lulus uji lokal (`pytest` & ML validation), buat Pull Request dari `development` menuju `main`.
3. GitHub Actions CI akan berjalan otomatis:
   - Flake8 syntax check
   - Pytest 58/58 tests
   - ML model cross-validation
   - Robustness & 2-skema verification
4. **Peer Review Manusia**: Anggota tim lain / reviewer melakukan review kode dan memberikan status **Approve**.
5. Setelah CI hijau dan Review Manusia disetujui, PR baru dapat di-merge ke `main`.
