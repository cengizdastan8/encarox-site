# Kesit

Android için hafif video kesme aracı. Telefondaki hantal kesicilerin üç derdine
doğrudan cevap veriyor: **beklemek**, **kesmeden önce göremeyip sonra pişman olmak**
ve **hassas noktayı tutturamamak**.

## Ne yapıyor

| İstek | Nasıl çözülüyor |
| --- | --- |
| "Yüklemesi 500 saat sürüyor" | Video hiç yeniden kodlanmıyor. Kayıpsız modda sıkıştırılmış kareler doğrudan yeni kaba kopyalanıyor; hassas modda yalnızca ilk kare grubu (GOP) yeniden kodlanıp gerisi kopyalanıyor. Kaydetme birkaç saniye. |
| "Kestiğim yeri izleyemiyorum" | **Kesiti izle** düğmesi seçili aralığı döngüde oynatıyor. Bunun için dosya üretilmiyor — oynatıcıya kırpma aralığı veriliyor, o yüzden anında başlıyor. Beğenirsen kaydediyorsun. |
| "Milimetrik kesemiyorum" | Zaman çizgisi yakınlaştırılabilir. Yakınlaştıkça bir piksel daha az milisaniye eder; ayrıca ±1 kare / ±100 ms / ±1 sn tuşları var. Tutamacı çektiğin an oynatıcı o kareyi gösteriyor. |
| "Kalitesi ve sesi bozulmasın" | Video akışı olduğu gibi kopyalanıyor, ses hiçbir modda yeniden kodlanmıyor. Çözünürlük, bit hızı, kodek kaynakla aynı kalıyor. |
| "Önceki video yok olmasın" | Kaynak dosyaya hiç dokunulmuyor. Her kesit `Movies/Kesit` altına yeni bir dosya olarak yazılıyor; aynı videodan istediğin kadar kesit alabilirsin. |

## İki kesme modu

**Hassas (varsayılan)** — Media3 Transformer, `trim optimization` açık. Başlangıç
tam istediğin karede. Yalnızca başlangıç ile ondan sonraki ilk anahtar kare
arasındaki bölüm yeniden kodlanır (genelde bir saniyeden az), kalan her şey
kopyalanır.

**Kayıpsız** — `MediaExtractor` + `MediaMuxer` ile saf akış kopyası. Tek bir kare
bile yeniden kodlanmaz, sonuç bit bit aynıdır ve işlem anlıktır. Bedeli: video
ancak bir anahtar kareden başlayabilir, yani başlangıç istediğin noktadan biraz
geride kalabilir. Uygulama ne kadar kaydığını kaydettikten sonra milisaniye
cinsinden söyler.

Hassas mod bir videoda çalışmazsa (kodek/kap uyumsuzluğu) uygulama sessizce
kayıpsız moda geçip bunu sonuç ekranında bildirir; elin boş kalmaz.

## Kurulum

APK'yı derlemeye gerek yok: her `kesit/` değişikliğinde
[Actions](../../actions/workflows/kesit-apk.yml) sekmesindeki **Kesit APK** işi
çalışıyor. Son çalışmayı aç, sayfanın altındaki `kesit-apk` arşivini indir,
içinden `app-release.apk` dosyasını telefona at ve kur (bilinmeyen kaynaklara
izin vermen gerekebilir). Release sürümü takılırsa arşivdeki `app-debug.apk`
her zaman çalışır.

Kendi bilgisayarında derlemek için Android Studio ile `kesit/` klasörünü aç, ya da:

```bash
cd kesit
gradle assembleRelease     # veya ./gradlew assembleRelease
```

Gerekenler: JDK 17, Android SDK 35, Gradle 8.9+.

### Sabit imza (isteğe bağlı)

İmza anahtarı verilmezse release APK debug anahtarıyla imzalanır; bu durumda
yeni sürümü üstüne kurarken önce eskisini kaldırman gerekebilir. Sabit bir
anahtar için repo ayarlarına şu secret'ları ekle: `KESIT_KEYSTORE_B64`
(keystore dosyasının base64'ü), `KESIT_KEYSTORE_PASSWORD`, `KESIT_KEY_ALIAS`,
`KESIT_KEY_PASSWORD`.

## Kullanım

1. **Video seç** — sistem foto seçici açılır, hiçbir depolama izni istenmez.
   (Galeriden "Paylaş → Kesit" de çalışır.)
2. Zaman çizgisindeki iki tutamacı sürükle. Üstteki ince şerit videonun tamamı;
   ona dokunarak yakınlaştırılmış pencereyi gezdirebilirsin.
3. Hassasiyet için **+** ile yakınlaş, ya da **Seçime** ile doğrudan seçime otur.
   Başlangıç/bitiş kutusuna dokunup hangi tutamacı ayarladığını seç, sonra
   ±1 kare tuşlarıyla ince ayar yap.
4. **Kesiti izle** — seçili aralık döngüde oynar.
5. **Kaydet** — `Movies/Kesit` klasörüne yazılır, kaynak yerinde kalır.

## Gereksinimler

- Android 8.0 (API 26) ve üzeri
- Depolama izni yok (Android 10+). Android 9 ve öncesinde yalnızca kayıt klasörü
  için yazma izni istenir.

## Yapı

```
app/src/main/java/com/encarox/kesit/
  MainActivity.kt            Giriş, video seçimi, paylaş/aç niyetleri
  media/
    VideoInfo.kt             Süre, kare hızı, döndürme, kodek tanıma
    Filmstrip.kt             Zaman çizgisi kare önizlemeleri
    LosslessTrimmer.kt       MediaExtractor + MediaMuxer akış kopyası
    PreciseTrimmer.kt        Media3 Transformer, trim optimization açık
    MediaStoreWriter.kt      Movies/Kesit altına kayıt
    TrimEngine.kt            İş akışı, mod seçimi, geri düşüş
  ui/
    Timeline.kt              Yakınlaştırılabilir çift katlı zaman çizgisi
    EditorController.kt      Oynatıcı ve editör durumu
    EditorScreen.kt          Editör arayüzü
    HomeScreen.kt            Karşılama ekranı
```

Uygulamada FFmpeg yok. Her şey Android'in kendi donanım hızlandırmalı medya
katmanı ve Jetpack Media3 üzerinde çalışıyor — APK bu yüzden küçük ve açılış
anlık.
