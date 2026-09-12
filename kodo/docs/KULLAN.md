# Kodo'yu telefonuma nasıl kurar ve kullanırım? (adım adım)

Bu rehber teknik bilgi gerektirmez. ~15-20 dakika sürer, bir kere yapılır.

Uygulama iki kaynaktan kod toplar:
- **SMS** → hiçbir kuruluma gerek yok, direkt çalışır.
- **Gmail** → çalışması için (aşağıdaki) **Adım 2**'deki Google ayarını 1 kez yapman gerekir.

---

## Adım 1 — APK dosyasını indir (uygulamanın kurulum dosyası)

GitHub bizim için APK'yı otomatik üretiyor.

1. Şu adrese gir (bilgisayardan daha kolay):
   `https://github.com/cengizdastan8/encarox-site/actions/workflows/kodo-android.yml`
2. En üstteki yeşil ✓ işaretli çalışmaya tıkla.
3. Sayfanın altında **"Artifacts"** bölümünde **`kodo-debug-apk`** dosyasını indir (bir .zip iner).
4. Zip'i aç → içinden **`app-debug.apk`** çıkar.
5. Bu `app-debug.apk` dosyasını telefonuna gönder (WhatsApp/Telegram kendine, Drive, USB — fark etmez).
6. Telefonda dosyaya dokun → "Bilinmeyen kaynaklara izin ver" çıkarsa izin ver → **Yükle**.

> Uygulama telefonda **"Kodo"** adıyla görünür.

---

## Adım 2 — Gmail'i aç (Google güvenlik ayarı, 1 kere)

Gmail okuması Google'ın izin sistemini kullanır. Kendi Google projeni oluşturman gerekiyor
(uygulamaya gömülü şifre yok — Google seni telefonundaki uygulamaya "parmak izi"yle bağlar).

1. `https://console.cloud.google.com/` → giriş yap → üstten **yeni proje** oluştur, adı: **Kodo**.
2. Sol menü **APIs & Services → Library** → arat: **Gmail API** → **Enable**.
3. **APIs & Services → OAuth consent screen**:
   - User type: **External** → Create.
   - Uygulama adı: Kodo, destek e-postası: kendi e-postan.
   - **Scopes** adımında ekle: `.../auth/gmail.readonly`
   - **Test users** adımında: **kod toplamak istediğin bütün Gmail adreslerini ekle** (5-6 tanesi de olur).
4. **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Application type: **Android**
   - **Package name:** `com.encarox.kodo.debug`   ← (debug sürümü için, aynen böyle)
   - **SHA-1:** aşağıdaki değeri aynen yapıştır:
     ```
     56:F2:60:43:DF:D0:64:9B:22:D3:C3:52:D1:D2:0D:92:B3:2D:10:F5
     ```
   - **Create**.

> Bu SHA-1 sabittir (imza repoya gömülü). Yani bir kere girersin, her yeni APK'da da çalışır.

---

## Adım 3 — Uygulamayı kullan

1. Kodo'yu aç → **Başla**.
2. **Hesap ekle** → bir Gmail seç → "izin ver" (Google "doğrulanmamış uygulama" uyarısı
   gösterebilir; sen test kullanıcısı olduğun için **Advanced → devam et** ile geçebilirsin).
   Bunu bütün Gmail hesapların için tekrarla.
3. **SMS için:** Ayarlar → **"Sıradaki SMS kodunu dinle"** → sana kod SMS'i geldiğinde
   Android tek dokunuşluk onay gösterir, onayla → kod Kodo'ya düşer.
4. Artık kod geldiğinde **bildirim** gelir; **"Kopyala"**ya basıp yapıştırırsın. 🎉

---

## Notlar
- **Her şey telefonunda kalır.** Hiçbir sunucuya veri gitmez.
- Gmail tarafı test modunda 100 hesaba kadar sınırsız çalışır. Uygulamayı Play'de herkese
  açmak istersen Google'ın CASA güvenlik denetimi gerekir (bkz. `PLAY_STORE.md`) —
  kendi kullanımın için buna gerek yok.
- Kod gelmiyorsa: Ayarlar'dan Gmail kontrol aralığını düşür, ya da hesabın "taranıyor"
  durumda olduğundan emin ol.
