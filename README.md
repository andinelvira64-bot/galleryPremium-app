# Gallery App

Aplikasi galeri Android (Java) untuk membuka foto (JPG/PNG/WebP/HEIC/BMP/GIF)
dan video (MP4/3GP/MKV/WebM/MOV/AVI/M4V), dengan 4 efek tampilan seperti di
referensi screenshot: **Auto Coloring**, **Enable Blur Effect**,
**Enable Glow Effect**, **Enable Parallax Effect**.

## Cara scan folder bekerja

`MediaScanner.java` memindai satu folder root (default: `Download`) dan
**semua subfolder di dalamnya, berapa pun dalamnya**, menggunakan stack
eksplisit (bukan rekursi biasa) supaya tidak crash walau folder-nya sangat
dalam atau isinya banyak. Jadi struktur seperti:

```
Download/fkdos/djdiejd/foto.jpg
Download/apapunNamanya/lebihDalamLagi/video.mp4
```

akan otomatis terdeteksi, tidak peduli nama foldernya apa.

Untuk bisa membaca folder sembarang (bukan cuma yang sudah diindeks
MediaStore), app minta izin **"Akses semua file"** (All Files Access) di
Android 11+. Ini wajib diaktifkan manual sekali di halaman pengaturan yang
akan otomatis terbuka saat pertama kali app dijalankan.

Kalau kamu mau ganti folder root yang di-scan (misalnya DCIM, atau
seluruh /storage/emulated/0), tinggal edit baris ini di `MainActivity.java`:

```java
File downloadRoot = new File(Environment.getExternalStorageDirectory(), "Download");
```

## Fitur efek viewer (ViewerActivity.java)

- **Auto Coloring** — ambil warna dominan dari foto/frame video yang sedang
  dilihat pakai library Palette, dipakai sebagai warna glow.
- **Enable Blur Effect** — buat backdrop blur dari foto/video yang sedang
  dilihat (stack blur murni Java, tanpa RenderScript yang sudah deprecated).
- **Enable Glow Effect** — overlay gradient warna dominan di belakang media.
- **Enable Parallax Effect** — backdrop bergeser sedikit mengikuti
  kemiringan HP (pakai sensor accelerometer).

Semua toggle disimpan di `SettingsPrefs.java` (SharedPreferences) dan bisa
diubah dari layar Pengaturan (ikon di menu kanan atas).

## Build APK

Push project ini ke GitHub, lalu GitHub Actions (`.github/workflows/build.yml`)
akan otomatis build APK debug dan upload sebagai artifact bernama
`gallery-app-debug`. Tidak perlu gradle wrapper lokal — workflow install
Gradle sendiri lewat `gradle/actions/setup-gradle`.

Kalau mau build manual di Termux:

```bash
pkg install gradle openjdk-17
gradle assembleDebug
```

APK hasil build ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Struktur project

```
app/src/main/java/com/elvira/gallery/
  MainActivity.java       -> grid galeri + permission + trigger scan
  MediaScanner.java       -> scan rekursif tanpa batas kedalaman
  MediaItem.java          -> model 1 file foto/video
  MediaAdapter.java       -> RecyclerView grid thumbnail (pakai Glide)
  ViewerActivity.java     -> viewer fullscreen + 4 efek
  MediaPagerAdapter.java  -> ViewPager2 untuk swipe antar foto/video
  SettingsActivity.java   -> layar toggle 4 efek
  SettingsPrefs.java      -> penyimpanan on/off tiap efek
  BlurUtils.java          -> stack blur pure Java
```
