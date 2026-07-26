# Release İmzalama Rehberi (kendi anahtarınla APK)

Bu rehber tek şeyi anlatır: **debug anahtarı yerine kendi release anahtarınla
imzalanmış APK üretmek.** Google Play'de yayınlamak gerekmez; APK GitHub
Releases'ten dağıtılmaya devam eder.

---

## 0. Şimdiye kadar neden sorun çıktı?

Eski `app/build.gradle.kts`, release derlemesinde keystore bilgisi bulamazsa
**sessizce debug anahtarına düşüyordu**. Sonuçları:

| Belirti | Sebep |
|---|---|
| Play Protect "bilinmeyen geliştirici / zararlı olabilir" uyarısı | Debug anahtarı, dağıtım için tasarlanmamış, güvenilmeyen bir imzadır |
| "Uygulama yüklenemedi" / üzerine güncelleme kurulmuyor | GitHub Actions runner'ı **her çalıştırmada yeni bir debug keystore üretir** → her sürüm **farklı** anahtarla imzalandı. Android, imzası değişen uygulamanın üzerine güncelleme kurmaz |
| APK "hata ayıklanabilir" görünüyor | Debug keystore, `android`/`androiddebugkey` gibi herkesçe bilinen sabit değerleri kullanır; imzayı isteyen herkes taklit edebilir |

Artık **düşme yok**: gerçek anahtar tanımlı değilse `assembleRelease` açık bir
hata mesajıyla durur (`verifyReleaseSigning` görevi). Bilinçli test için
`-PallowDebugSigning=true` gerekir; o çıktının adı `…-TEST-debugsigned.apk` olur
ve CI onu tag/release'e çevirmez, yalnızca artifact bırakır.

> **Mevcut kullanıcılar için — tek seferlik:** İlk düzgün imzalı sürüme geçerken
> eski (debug imzalı) APK'yı kurmuş herkes uygulamayı **kaldırıp yeniden kurmak**
> zorunda. Bundan sonra imza sabit kaldığı için güncellemeler sorunsuz üste kurulur.
> Bunu sürüm notuna yaz.

---

## 1. Anahtarı (keystore) üret

Repo **dışında** bir klasörde:

```bash
mkdir -p ~/keys && cd ~/keys

keytool -genkeypair -v \
  -keystore dpibypass-release.jks \
  -alias dpibypass \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

Sorulanlar:

- **Parola** — güçlü olsun, parola yöneticine kaydet. (PKCS12'de store ve key
  parolası aynıdır, `keytool` ikincisini ayrıca sormaz.)
- **Ad ve soyad (CN)** — `DPI Bypass` yazabilirsin; gerçek ad zorunlu değil.
- **Birim / kurum / şehir / ülke** — `TR` vb. Sonradan değiştirilemez ama
  kullanıcı deneyimini etkilemez.
- `-validity 10000` ≈ 27 yıl. Uzun tut; sertifika süresi dolarsa güncelleme
  yayınlayamazsın.

Doğrula ve parmak izini not et:

```bash
keytool -list -v -keystore ~/keys/dpibypass-release.jks -alias dpibypass
# "Certificate fingerprints > SHA-256" satırını kaydet.
```

> ### Bu dosyayı KAYBETME
> Play App Signing kullanmadığın için **tek kopya sende**. Kaybedersen ya da
> parolasını unutursan, aynı uygulamaya bir daha güncelleme yayınlayamazsın —
> yeni anahtarla çıkan sürüm kullanıcıya "farklı uygulama" gibi görünür ve
> herkesin elle kaldırıp yeniden kurması gerekir.
> - Repoya **koyma** (`.gitignore` `*.jks` ve `keystore.properties` içeriyor).
> - En az iki yerde şifreli yedekle (parola yöneticisi + harici disk).

---

## 2. Yerel makinede kullan

```bash
cd /path/to/DPI-Bypass-DC
cp keystore.properties.example keystore.properties
```

`keystore.properties` içini doldur:

```properties
storeFile=/home/KULLANICI/keys/dpibypass-release.jks
storePassword=...
keyAlias=dpibypass
keyPassword=...
```

Derle:

```bash
./gradlew assembleRelease
```

Log'da şunu görmelisin:

```
Release imzası: RESMİ anahtar (alias=dpibypass).
```

İmzayı doğrula:

```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

`Signer #1 certificate SHA-256 digest` değeri §1'de not ettiğinle **aynı** olmalı.
`keystore.properties` git'e girmez.

---

## 3. GitHub Actions'a bağla

Keystore'u base64'e çevir (satır sonu **olmadan**):

```bash
# Linux
base64 -w0 ~/keys/dpibypass-release.jks > ~/keys/keystore.b64
# macOS
base64 -i ~/keys/dpibypass-release.jks -o ~/keys/keystore.b64
```

GitHub → repo → **Settings → Secrets and variables → Actions → New repository secret**.
Dört tane ekle:

| Secret adı | Değer |
|---|---|
| `KEYSTORE_BASE64` | `keystore.b64` dosyasının tüm içeriği (tek satır) |
| `KEYSTORE_PASSWORD` | keystore parolası |
| `KEY_ALIAS` | `dpibypass` |
| `KEY_PASSWORD` | anahtar parolası (PKCS12'de store parolasıyla aynı) |

Sonra yerel `keystore.b64` dosyasını sil:

```bash
shred -u ~/keys/keystore.b64 2>/dev/null || rm -f ~/keys/keystore.b64
```

---

## 4. Sürüm çıkarma akışı

**Actions → Release → Run workflow** → sürüm gir (ör. `2.2.0`) → çalıştır.

Workflow sırasıyla:

1. Sürümü SemVer olarak doğrular, `versionCode` hesaplar, `gradle.properties`'i günceller
2. Testleri ve lint'i çalıştırır (kapı)
3. **Keystore'u hazırlar** — secret eksikse `::error::` verip **durur**;
   base64'ü çözer ve `keytool -list` ile parolayı/alias'ı gerçekten açarak doğrular
4. `assembleRelease` — resmi anahtarla imzalar
5. `apksigner verify --print-certs` ile imza sahibini log'a yazar
   (hangi anahtarla çıktığı sürüm kaydında görünür)
6. Sürüm bump commit'i + `v2.2.0` etiketi + GitHub Release (APK ekli)

### Test derlemesi (dağıtım değil)

Workflow'daki **"SADECE TEST: debug anahtarıyla imzala"** kutusu işaretlenirse:
keystore secret'ları atlanır, APK debug anahtarıyla imzalanır ve **tag/release
oluşturulmaz** — APK yalnızca `…-TEST-debugsigned.apk` adıyla artifact olur.
Yerelde karşılığı:

```bash
./gradlew assembleRelease -PallowDebugSigning=true
```

---

## 5. Sık karşılaşılan hatalar

| Hata | Sebep / çözüm |
|---|---|
| `Release imzalama anahtarı bulunamadı — derleme durduruldu` | Dört değerden en az biri eksik. Yerelde `keystore.properties`, CI'da secret'lar |
| CI: `Keystore açılamadı` | `KEYSTORE_BASE64` satır sonu içeriyor (`base64 -w0` kullan) veya parola/alias yanlış |
| `Failed to read key ... wrong password` | `keyPassword` ≠ `storePassword`. PKCS12'de ikisi aynıdır; JKS'ten dönüştürdüysen farklı olabilir |
| Kullanıcıda "Uygulama yüklenmedi" | Eski debug imzalı sürüm kurulu — bir kez kaldırıp yeniden kurmalı |
| Yerelde `storeFile` bulunamıyor | Göreli yol `app/` klasörüne göre çözülür; **mutlak yol** yaz |
| Anahtarı değiştirdim, güncelleme geçmiyor | Android imza değişimine izin vermez. Anahtar sabit kalmalı — bu yüzden yedek şart |

---

## 6. "Google'ın anahtarıyla imzalatmak" mümkün mü?

Kısa cevap: **bağımsız bir "Google APK imzalama" hizmeti yok.** Google'ın anahtarı
işin içine yalnızca tek bir yolla girer: **Play App Signing** — ve o da Play
Console'dan geçer.

Nasıl işlediği:

```
Senin ürettiğin anahtar   =  YÜKLEME (upload) anahtarı
   → AAB'yi bununla imzalarsın, Play seni bununla tanır
Google'ın ürettiği anahtar =  UYGULAMA İMZA (app signing) anahtarı
   → Kullanıcıya giden APK'yı Google bununla imzalar
```

Dikkat: **üstteki §1–§3 bu senaryoda da aynen gerekli.** Google'ın imzalaması,
senin kendi anahtarını ortadan kaldırmaz; yükleme anahtarı yine sensin.

Play'de *yayınlamadan* Google imzalı APK almak teknik olarak mümkündür ama
maliyeti var:

1. Play Console hesabı aç (tek seferlik 25 USD) + kimlik doğrulaması yaptır.
2. Uygulamayı oluştur (`net.atom.dpibypass`), Play App Signing'i kabul et.
3. `bundleRelease` ile AAB üret, **iç test (internal testing)** sürümüne yükle.
4. **Uygulama paketi gezgini (App bundle explorer)** → ilgili sürüm →
   *İmzalı, birleşik (universal) APK indir*. İnen APK Google'ın anahtarıyla
   imzalıdır; onu GitHub Releases'te dağıtabilirsin.

Bunun bedeli: her sürümde elle yapılan bir tur, Play politikalarının kapsamına
girmek (VPN beyanı, gizlilik politikası, `QUERY_ALL_PACKAGES` incelemesi) ve
uygulamanın Play tarafından reddedilme ihtimali.

**Ama asıl mesele şu:** Google'ın gördüğün uyarıları vermesinin sebebi "APK
Google tarafından imzalanmamış" değil. Sebep, APK'nın **debug anahtarıyla** ve
**her sürümde farklı bir anahtarla** imzalanmış olmasıydı. Sideload edilen
uygulamaların ezici çoğunluğu (Signal, F-Droid uygulamaları, ByeDPI, NekoBox…)
geliştiricinin kendi anahtarıyla imzalanır — normal ve doğru olan budur.
§1–§3'ü uyguladığında Play Protect'in "bilinmeyen geliştirici" davranışı
değişir; ileriye dönük tam çözüm ise §7'deki geliştirici doğrulamasıdır.

---

## 7. Google'ın geliştirici doğrulaması (sideload'u da kapsıyor)

Play'de yayınlamasan bile Google, 2026'dan itibaren kademeli olarak sertifikalı
cihazlara kurulan uygulamaların **doğrulanmış bir geliştiriciye** ait olmasını
isteyecek — sideload dahil. İlk dalga Brezilya/Endonezya/Singapur/Tayland;
Türkiye sonraki dalgalarda. O noktada **Android Developer Console** üzerinden
kimliğini doğrulayıp paket adını (`net.atom.dpibypass`) kaydedersin ve sahipliği
**bu rehberde ürettiğin anahtarla imzalanmış** bir APK vererek kanıtlarsın.
Yani anahtarı şimdi düzgün kurmak, o gün için de hazırlık oluyor.
Kendi cihazına ADB ile kurmak her hâlükârda serbesttir.
