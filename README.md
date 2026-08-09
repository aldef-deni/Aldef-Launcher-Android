# ALDEF LAUNCHER

Launcher Android bergaya J.A.R.V.I.S. dengan asisten suara AI.
Dibuat dengan **Kotlin + Jetpack Compose** (native).

Target: **Realme 5 Pro** (Android 9–11) — minSdk 26, targetSdk 35.

---

## Tampilan

```
────────────────────────
        ALDEF LAUNCHER  ⚙

           08:45
    SENIN, 9 AGUSTUS 2026

       SELAMAT PAGI
           DENI
────────────────────────
 ⚡ POWER        🛰 NETWORK
 Baterai 82%     5G

 🧠 AI STATUS    🌤 WEATHER
 ONLINE          26°  Cerah

 📍 LOCATION
 Jakarta
────────────────────────
       (arc reactor)
    KETUK UNTUK BICARA

    ── GESER ATAS · APLIKASI
```

---

## Cara memasang ke HP

### Cara 1 — lewat Android Studio (paling gampang)
1. Buka Android Studio → **File ▸ Open** → pilih folder `Aldef Launcher`.
2. Tunggu Gradle sync selesai.
3. Aktifkan **USB Debugging** di HP: Setelan ▸ Tentang ponsel ▸ ketuk "Versi realme UI" 7×,
   lalu Setelan ▸ Setelan tambahan ▸ Opsi pengembang ▸ **USB debugging** ON.
4. Colok HP ke laptop, pilih **Transfer file (MTP)**, izinkan permintaan debugging.
5. Tekan tombol **Run ▶**.

### Cara 2 — pasang APK langsung
APK debug sudah jadi di:

```
app\build\outputs\apk\debug\app-debug.apk
```

Pasang lewat kabel:
```powershell
cd "c:\Users\ade zulham\Downloads\Aldef Launcher"
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Atau salin `app-debug.apk` ke HP lewat WhatsApp/kabel, lalu ketuk file-nya
(izinkan "Install dari sumber tidak dikenal").

### Build ulang dari terminal
```powershell
cd "c:\Users\ade zulham\Downloads\Aldef Launcher"
.\gradlew.bat assembleDebug
```

---

## Setelah terpasang

1. Ketuk ikon **ALDEF LAUNCHER**. Yang terbuka adalah **Aldef Panel** — layar HUD
   berisi emblem heksagon, status sistem, saklar ON/OFF, dan seluruh konfigurasi.
2. Saat pertama kali dibuka, muncul **modal identifikasi**: isi nama Anda. Nama itu
   tampil sebagai "SELAMAT DATANG, ⟨NAMA⟩" di panel dan dipakai Aldef saat menyapa.
3. Geser saklar **ALDEF INTERFACE** ke **ON**. Urutannya:
   izin (mikrofon + lokasi) → animasi boot → sistem dimulai ulang → HUD tampil.
4. Tekan tombol **Home** → pilih **ALDEF LAUNCHER** ▸ **Selalu**, atau tekan
   **PILIH LAUNCHER UTAMA** di bagian konfigurasi.

Layar depan HUD sengaja tidak punya tombol pengaturan. Untuk kembali ke Aldef Panel,
geser ke atas lalu ketuk ikon **ALDEF LAUNCHER** yang ada di urutan pertama laci
aplikasi.

Mematikan saklar mengembalikan Aldef ke mode standby; MainActivity akan menampilkan
panel "ANTARMUKA HUD NONAKTIF" dengan tombol untuk membuka Aldef Panel lagi.

### Zona waktu dan sapaan

Sapaan mengikuti **waktu Indonesia**, bukan sekadar jam perangkat. Zona ditentukan
di [IndonesianTime.kt](app/src/main/java/com/aldef/launcher/core/IndonesianTime.kt):
zona bawaan perangkat dipakai bila sudah zona Indonesia (paling akurat, karena
sistem tahu batas provinsi sebenarnya); kalau bukan, zona dihitung dari bujur GPS
selama titiknya masih di dalam Indonesia. Labelnya (WIB / WITA / WIT) tampil di
samping tanggal, dan jam pada HUD memakai zona yang sama supaya tidak bertentangan.

Batas sapaan: pagi 04–10, siang 11–14, sore 15–17, malam 18–03.

> ⚠️ **Soal "restart Android otomatis":** aplikasi pihak ketiga **tidak bisa**
> me-reboot perangkat — `PowerManager.reboot()` butuh izin `REBOOT` yang bertanda
> tangan sistem, jadi hanya app bawaan ROM atau perangkat root yang boleh.
> [SystemRestarter.kt](app/src/main/java/com/aldef/launcher/core/SystemRestarter.kt)
> tetap mencoba reboot sungguhan (berhasil di ROM kustom), lalu jatuh ke
> **restart proses aplikasi penuh** — proses dimatikan dan dijalankan ulang, sehingga
> HUD tetap dimulai dari state bersih tanpa risiko crash saat berganti tampilan.

### Balik ke launcher realme
Setelan ▸ Aplikasi ▸ Aplikasi default ▸ Aplikasi layar utama ▸ pilih **Launcher**.

---

## Cara pakai

| Aksi | Cara |
|---|---|
| Bicara ke Aldef | Ketuk **arc reactor** di tengah |
| Dengar sapaan lagi | Ketuk nama Anda ("DENI") |
| Buka daftar aplikasi | **Geser ke atas**, atau ketuk garis di bawah |
| Cari aplikasi | Buka laci aplikasi → ketik di kolom pencarian |
| Info / copot aplikasi | **Tahan lama** ikon aplikasi di laci |
| Pengaturan | Ikon ⚙ kanan atas |

### Perintah suara yang jalan tanpa internet & tanpa API key
- `buka whatsapp` / `buka kamera` / `jalankan youtube`
- `baterai` — sisa daya + status charging
- `jam berapa` · `tanggal berapa`
- `cuaca` — suhu & kondisi di lokasi Anda
- `jaringan` — 5G / LTE / Wi-Fi
- `senter` — nyala/mati (toggle)
- `wifi` · `bluetooth` · `pengaturan` · `alarm`
- `cari resep rendang` — buka pencarian Google
- `telepon 08123456789` — buka dialer

Perintah/pertanyaan lain ("jelaskan fotosintesis", "buatkan pantun") diteruskan ke AI.

---

## Hasil uji di emulator

Diuji pada AVD 1080×2340 @440dpi (setara Realme 5 Pro), Android 16, GPU host:

| Fitur | Hasil |
|---|---|
| Panel aktivasi + saklar ON/OFF | ✅ |
| Alur ON → izin → animasi boot → restart → HUD | ✅ |
| HUD, jam, sapaan, tanggal Indonesia | ✅ |
| Baterai & jaringan | ✅ (Wi-Fi, 100%) |
| Cuaca Open-Meteo | ✅ 26° Cerah berawan |
| Lokasi tingkat jalan | ✅ "Jalan Tugu Monas No. 1 · Gambir, Kota Jakarta Pusat · ±5 m" |
| Muat ulang GPS tiap 5 menit | ✅ (langganan LocationManager + penarikan berkala) |
| Empat kartu status, dua per baris | ✅ |
| Jam digital tujuh-ruas (ruas mati tetap samar, titik dua berkedip, detik kecil) | ✅ |
| Arc reactor beranimasi | ✅ |
| Laci aplikasi + pencarian | ✅ 20 aplikasi |
| Ikon aplikasi gaya HUD | ✅ oktagon + monokrom sian |
| Geser atas → laci | ✅ dari mana saja, termasuk di atas reactor |
| Ketuk reactor → mikrofon | ✅ |
| Sapaan TTS | ✅ (teks tampil; suara tidak terdengar di emulator) |
| Perintah suara | ⚠️ tidak bisa diuji di emulator — tidak ada mikrofon. Uji di HP asli. |
| Reboot perangkat sungguhan | ⚠️ tidak mungkin di HP non-root; jatuh ke restart proses (lihat catatan di atas) |

> Catatan: emulator sempat memunculkan dialog *"System UI isn't responding"*. Itu
> masalah SystemUI bawaan emulator (log crash aplikasi kosong), bukan Aldef.
> Kalau muncul, tekan **Wait**.

## Mengaktifkan otak AI (opsional)

> ⚠️ **Kolom API key sedang disembunyikan** atas permintaan. Semua kode AI
> ([ClaudeClient.kt](app/src/main/java/com/aldef/launcher/ai/ClaudeClient.kt) dan
> jalur `NeedsAi` di [Brain.kt](app/src/main/java/com/aldef/launcher/ai/Brain.kt))
> masih utuh — yang dilepas hanya kolom isiannya di bagian konfigurasi. Untuk
> menampilkannya kembali, tambahkan satu `HudTextField` untuk `prefs.apiKey` di
> `ConfigurationPanel` pada
> [SetupScreen.kt](app/src/main/java/com/aldef/launcher/ui/SetupScreen.kt).

Tanpa API key, Aldef tetap berfungsi penuh untuk semua perintah perangkat di atas.
Selama key kosong, pertanyaan bebas dijawab dengan pesan bahwa fitur AI belum aktif.

Model yang dipakai: `claude-opus-5` (lihat [`ClaudeClient.kt`](app/src/main/java/com/aldef/launcher/ai/ClaudeClient.kt)).

> ⚠️ **Keamanan:** API key disimpan di `SharedPreferences` HP ini. Untuk pemakaian
> pribadi tidak masalah. Kalau APK ini mau disebar ke orang lain, key **jangan**
> ditaruh di aplikasi — buat server perantara kecil, aplikasi memanggil server itu,
> server yang menyimpan key. Siapa pun yang pegang APK berisi key bisa memakai
> kuota Anda.

---

## Struktur kode

```
app/src/main/java/com/aldef/launcher/
├── SetupActivity.kt         Panel aktivasi — SATU-SATUNYA ikon di laci aplikasi
├── MainActivity.kt          Layar HOME (tanpa CATEGORY_LAUNCHER, tak jadi ikon kedua)
├── LauncherViewModel.kt     Semua state HUD + orkestrasi
├── ai/
│   ├── Brain.kt             Router perintah (lokal dulu, AI belakangan)
│   └── ClaudeClient.kt      Panggilan Anthropic Messages API
├── core/
│   ├── AppRepository.kt     Daftar & peluncuran aplikasi, pencocokan nama
│   ├── LocationRepository.kt GPS berkala + alamat tingkat jalan (tanpa Play Services)
│   ├── WeatherRepository.kt Open-Meteo (gratis, tanpa key)
│   ├── SystemMonitor.kt     Baterai & jaringan
│   ├── SystemRestarter.kt   Reboot perangkat / restart proses
│   └── Prefs.kt             SharedPreferences (termasuk saklar hudEnabled)
├── voice/
│   ├── Speaker.kt           Text-to-Speech
│   └── VoiceInput.kt        SpeechRecognizer
└── ui/
    ├── SetupScreen.kt       Layar aktivasi + saklar HUD + urutan boot
    ├── HudScreen.kt         Layar utama HUD (4 kartu status)
    ├── AppDrawerScreen.kt   Laci aplikasi + pencarian
    ├── SettingsScreen.kt    Pengaturan
    ├── components/
    │   ├── HudPieces.kt     Panel sudut-potong, kartu status, ikon HUD, arc reactor
    │   └── SwipeUp.kt       Gestur geser-atas + ketukan murni
    └── theme/               Palet & tipografi HUD
```

### Cara ikon aplikasi diubah jadi gaya HUD

Ikon asli tidak diganti gambar — ikon bawaan tiap aplikasi tetap dipakai, lalu
diproses ulang di [HudPieces.kt](app/src/main/java/com/aldef/launcher/ui/components/HudPieces.kt):
sebuah `ColorMatrix` menghitung luminansi ikon dan memetakannya ke kanal hijau-biru,
sehingga ikon jadi monokrom sian tapi bentuknya tetap dikenali. Bingkainya berupa
oktagon (sudut terpotong) yang digambar dengan `Canvas`. Konsekuensinya: aplikasi
apa pun yang dipasang di masa depan otomatis ikut bergaya HUD, tanpa perlu paket
ikon terpisah.

---

## Catatan teknis

- **Kenapa Kotlin native, bukan React Native/Flutter:** launcher butuh akses dalam ke
  `PackageManager`, broadcast baterai, `TelephonyManager`, TTS, `SpeechRecognizer`, dan
  torch — di RN/Flutter semuanya tetap harus ditulis sebagai native module, plus
  tambahan runtime JS/Dart yang memberatkan startup tiap kali tombol Home ditekan.
- **Ukuran APK 16 MB** sebagian besar dari SDK resmi Anthropic (Jackson + OkHttp).
  Kalau mau APK ~4 MB dan tidak butuh AI daring, hapus baris
  `implementation("com.anthropic:anthropic-java:2.34.0")` di `app/build.gradle.kts`
  beserta file `ai/ClaudeClient.kt` dan pemanggilannya di `LauncherViewModel.kt`.
- **Pengenalan suara butuh internet** (memakai layanan Google di HP). Text-to-speech
  jalan offline setelah paket bahasa Indonesia terpasang
  (Setelan ▸ Setelan tambahan ▸ Aksesibilitas ▸ Text-to-speech).
- **Label 5G** butuh izin `READ_PHONE_STATE`. Realme 5 Pro sendiri hanya 4G, jadi
  yang tampil normalnya `LTE`.
- Untuk rilis publik, jalankan `.\gradlew.bat assembleRelease` dan tanda tangani APK
  dengan keystore Anda sendiri.
