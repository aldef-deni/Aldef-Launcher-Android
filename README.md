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

1. Tekan tombol **Home** → Android bertanya mau pakai launcher yang mana → pilih
   **ALDEF LAUNCHER** ▸ **Selalu**.
   (Kalau tidak muncul: buka Aldef ▸ ikon ⚙ ▸ *Jadikan launcher utama*.)
2. Izinkan **Mikrofon** dan **Lokasi** saat diminta — mikrofon untuk perintah suara,
   lokasi untuk cuaca. Kalau lokasi ditolak, cuaca dipakai default Jakarta.
3. Buka ⚙ untuk mengganti nama panggilan, bahasa (Indonesia/English), dan API key.

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
| HUD, jam, sapaan, tanggal Indonesia | ✅ |
| Baterai & jaringan | ✅ (Wi-Fi, 100%) |
| Cuaca Open-Meteo | ✅ 26° Berawan |
| Lokasi (Geocoder) | ✅ Kota Jakarta Selatan |
| Arc reactor beranimasi | ✅ |
| Laci aplikasi + pencarian | ✅ 20 aplikasi |
| Geser atas → laci | ✅ dari mana saja, termasuk di atas reactor |
| Ketuk reactor → mikrofon | ✅ |
| Sapaan TTS | ✅ (teks tampil; suara tidak terdengar di emulator) |
| Perintah suara | ⚠️ tidak bisa diuji di emulator — tidak ada mikrofon. Uji di HP asli. |

> Catatan: emulator sempat memunculkan dialog *"System UI isn't responding"*. Itu
> masalah SystemUI bawaan emulator (log crash aplikasi kosong), bukan Aldef.
> Kalau muncul, tekan **Wait**.

## Mengaktifkan otak AI (opsional)

Tanpa API key, Aldef tetap berfungsi penuh untuk semua perintah perangkat di atas.
Untuk tanya-jawab bebas:

1. Ambil API key di <https://console.anthropic.com> (menu **API Keys**).
2. Buka Aldef ▸ ⚙ ▸ tempel key di kolom **ANTHROPIC API KEY** ▸ tutup dengan ✕.
3. Kartu **AI STATUS** berubah dari `LOCAL` jadi `ONLINE`.

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
├── MainActivity.kt          Activity HOME, izin, navigasi antar layar
├── LauncherViewModel.kt     Semua state HUD + orkestrasi
├── ai/
│   ├── Brain.kt             Router perintah (lokal dulu, AI belakangan)
│   └── ClaudeClient.kt      Panggilan Anthropic Messages API
├── core/
│   ├── AppRepository.kt     Daftar & peluncuran aplikasi, pencocokan nama
│   ├── LocationRepository.kt LocationManager + Geocoder (tanpa Play Services)
│   ├── WeatherRepository.kt Open-Meteo (gratis, tanpa key)
│   ├── SystemMonitor.kt     Baterai & jaringan
│   └── Prefs.kt             SharedPreferences
├── voice/
│   ├── Speaker.kt           Text-to-Speech
│   └── VoiceInput.kt        SpeechRecognizer
└── ui/
    ├── HudScreen.kt         Layar utama HUD
    ├── AppDrawerScreen.kt   Laci aplikasi + pencarian
    ├── SettingsScreen.kt    Pengaturan
    ├── components/          Arc reactor, kartu status, garis HUD
    └── theme/               Palet & tipografi HUD
```

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
