# Fossify Gallery Cipher — Güvenlik Sertleştirme ve Güvenli Silme Rehberi

Kapsam: statik kod incelemesi (CryptoManager, VirtualAlbumSyncManager, EncryptedMediaDataSource, Glide/ExoPlayer yolları, manifest, DOCS.md). Uygulama derlenip çalıştırılmadı. Platforma bağlı maddeler (SQLite derleme seçenekleri, MediaStore davranışı, fstrim zamanlaması) hedef cihazda doğrulanmalıdır.

## 1. Hedef ve dürüst tehdit modeli

Varsayımlar: güncel Android (FBE), yalnızca dahili depolama, root yok, kullanıcı her şifrelemeden sonra cihazı yeniden başlatıyor.

Korunması hedeflenenler:

- T1: root’suz kurtarma uygulamaları ve diğer uygulamalar
- T2: cihazı kaybeden veya çalan biri; cihaz yeniden başlatılmış, kilidi açılmamış (BFU)
- T3: yedekleme, paylaşım, ekran görüntüsü gibi yan kanallar
- T4: ayrıcalıklı adli erişim, cihaz açık ve kilidi bir kez açılmışken (AFU): yalnızca kısmi koruma

Garanti edilemeyenler: flash hücrelerinin fiziksel olarak silindiği; kamera uygulaması, bulut yedeği, mesajlaşma uygulamaları gibi dışarıdaki kopyalar; T4.

Yeniden başlatmanın etkisi: RAM temizlenir (anahtarlar, çözülmüş bitmap’ler, silinen dosyanın page cache sayfaları) ve cihaz BFU durumuna döner. Flash’taki bloklara dokunmaz ve fstrim’i garanti etmez. Kilit açılınca CE anahtarı yeniden yüklenir.

## 2. fstrim, discard ve çöp toplama (GC)

- Dosya silinince dosya sistemi blokları “boş” işaretler; flash denetleyicisi bunu discard/TRIM gelene kadar bilmez.
- İki yol vardır: F2FS’in discard mount seçeneği (checkpoint sırasında asenkron discard) ve idle maintenance sırasında (cihaz şarjda, ekran kapalı, boşta; genellikle günlük) çalışan fstrim ile F2FS çöp toplama. Mount seçenekleri ve zamanlama üreticiye göre değişir.
- Kapanışta /data unmount edilir ve F2FS son checkpoint’ini yapar; bekleyen discard’ların bir kısmı bu sırada gönderilebilir. Garanti değildir.
- Discard gelse bile denetleyici hücreyi hemen silmek zorunda değildir. Pratikte çoğu cihazda kısa sürede okunamaz hale gelir, ama standart bir garanti yoktur.
- FBE nedeniyle serbest kalan bloklar zaten şifreli içerik taşır; TRIM ikinci katmandır.
- Uygulama fstrim veya GC tetikleyemez. Yapılabilecekler: kullanıcıyı yönlendirmek (şarja tak, ekranı kapat) ve ileri düzey kullanıcılar için USB hata ayıklama ile `adb shell sm idle-maint run` (çoğu cihazda root’suz çalışır, yetki hatası verebilir; test edin).
- Boş alanı rastgele dosyalarla doldurma yöntemi önerilmez: flash’ı yıpratır, uzun sürer, cihazı kararsızlaştırabilir ve F2FS’in out-of-place yazımı yüzünden eski bloklara ulaşacağı garanti değildir.

## 3. Mevcut hatalar ve düzeltmeleri

### P0: hemen yapılmalı

**3.1 Güvenli silme (CryptoManager.secureWipeAndDeleteFile)**

Sorunlar: 64 MB üstündeki dosyalarda yalnızca 32 MB üzerine yazılıyor; yazılan tek geçiş sıfır (dokümandaki “rastgele + sıfır, çok geçişli” ifadesi kodla uyuşmuyor); hatalar yutuluyor ve dosya yine de siliniyor; fonksiyon sonuç döndürmüyor.

Düzeltme: dosyanın tamamı üzerine yazılır, `force(true)` çağrılır, kısaltılır, silinir. Hata olursa `false` döner ve çağıran orijinali silmez.

```kotlin
fun secureWipeAndDeleteFile(context: Context, file: File): Boolean {
    if (!file.exists()) return true
    val len = file.length()
    try {
        RandomAccessFile(file, "rw").use { raf ->
            val ch = raf.channel
            val buf = ByteArray(1 shl 20)
            var pos = 0L
            while (pos < len) {
                secureRandom.nextBytes(buf)
                val n = minOf(buf.size.toLong(), len - pos).toInt()
                var off = 0
                while (off < n) off += ch.write(ByteBuffer.wrap(buf, off, n - off), pos + off)
                pos += n
            }
            ch.force(true)
            ch.truncate(0)
            ch.force(true)
        }
    } catch (e: IOException) {
        return false   // çağıran orijinali silmemeli ve kullanıcıya bildirmeli
    }
    val deleted = file.delete()
    syncDir(file.parentFile)
    purgeTracesForFile(context, file.absolutePath)
    return deleted
}

private fun syncDir(dir: File?) {
    try {
        val fd = Os.open(dir!!.path, OsConstants.O_RDONLY, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    } catch (_: Exception) { /* en iyi çaba */ }
}
```

Notlar: tek geçiş yeterlidir; çoklu geçiş flash’ta fayda sağlamaz ve FBE altında yazılan sıfırlar da şifreli yazılır. Çağıranlar (encryptFolder, Activity.kt içindeki kopyala/taşı akışı) dönüş değerini kontrol etmeli, başarısızlıkta kullanıcıya hangi dosyaların güvenle silinemediğini göstermelidir.

**3.2 Silmeden önce doğrulama**

Şu an orijinal, şifreleme exception atmadığı için siliniyor. Şifrelerken düz metnin SHA-256 özetini hesaplayın; ardından şifreli dosyayı çözüp özeti karşılaştırın ve MAC’i doğrulayın. Uyuşmazlıkta şifreli dosyayı silin, orijinale dokunmayın.

```kotlin
// şifrelerken: her okunan düz metin bloğu için sha.update(buffer, 0, read)
val sha = MessageDigest.getInstance("SHA-256")
val expected = sha.digest()

// doğrularken
val check = MessageDigest.getInstance("SHA-256")
openDecryptedContentStream(enc)?.use { s ->
    val b = ByteArray(64 * 1024)
    while (true) { val n = s.read(b); if (n < 0) break; check.update(b, 0, n) }
}
val ok = verifyFileIntegrity(enc) && MessageDigest.isEqual(expected, check.digest())
if (!ok) { enc.delete(); return null }   // orijinale dokunma
```

**3.3 Kimlik bilgisi gücü ve .vault**

Sorun: PIN (en az 4 hane, yaklaşık 13 bit) ve desen (4–9 nokta, yaklaşık 18 bit) Argon2id’yi anlamsız kılar. `.vault` herkese açık Documents/Cipher altında; aynı sarılmış paket SharedPreferences’ta ve (allowBackup açık olduğu için) yedekte de duruyor. Offline kaba kuvvet saldırısı için gereken her şey saldırganın elinde.

Düzeltme:

- Günlük kullanım için cihaza bağlı sarma: sarılmış anahtar = GCM_Keystore( GCM_Argon2( MEK ) ). Keystore anahtarı varsa StrongBox’ta, yoksa TEE’de tutulur ve `setUnlockedDeviceRequired(true)` ile kilit açık olmadan kullanılamaz. Dosyalar kopyalansa bile PIN offline kırılamaz.
- Taşınabilirlik isteğe bağlı olsun: yalnızca güçlü bir parola ile (en az 6 diceware kelimesi, yaklaşık 77 bit) korunan ayrı bir kurtarma başlığı. Güçlü parola girilmediyse herkese açık dizine hiç başlık yazmayın.
- Argon2id: başlangıç 64 MiB, t=3, p=2 (RFC 9106’nın düşük bellek önerisine yakın); cihazda yaklaşık 0,5–1 sn sürecek şekilde kalibre edin.
- `unlockFromHeaderFile` başlıktaki parametreleri sınırsız okuyor; kabul aralığı koyun.

```kotlin
val spec = KeyGenParameterSpec.Builder(
    "vault_wrap", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setUnlockedDeviceRequired(true)
    .setIsStrongBoxBacked(hasStrongBox)   // yoksa TEE
    .build()

// başlık okurken
require(memoryKb in 8_192..262_144 && iterations in 1..10 && parallelism in 1..8)
```

**3.4 Uzun işlemlerin güvenliği**

Sorun: şifreleme ve silme `ensureBackgroundThread` içinde çalışıyor. Kullanıcı uygulamadan çıkarsa `onDestroy` içindeki `lockVault()` anahtarı işlem ortasında siler; süreç öldürülürse düz metin dosyalar kalır.

Düzeltme: işlemi foreground service (dataSync tipi) içinde çalıştırın; işlem sürerken otomatik kilidi askıya alın; bildirimde albüm adı göstermeyin; işlem öncesi boş alanı (albüm boyutu × 1,1) ve şarj durumunu kontrol edin.

**3.5 Alt klasörler**

`encryptFolder` yalnızca üst düzey dosyaları şifreliyor; alt klasörleri düz metin olarak `internalStorageRoot` altına taşıyor ve .nomedia’sını siliyor. Alt klasörleri özyinelemeli şifreleyin (tercih edilen) ya da işlemden önce kullanıcıya açık uyarı gösterip onay isteyin.

**3.6 Ekran görüntüsü ve Recents**

`FLAG_SECURE` hiç kullanılmıyor; Android uygulama arka plana gidince görev anlık görüntüsünü diske yazar. Vault görünümü (mIsSecretVaultView) ve şifreli medya açıkken koruma açılmalı.

```kotlin
fun Activity.applyVaultScreenProtection(enable: Boolean) {
    if (enable) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!enable)
}
```

**3.7 Yedekleme**

`allowBackup="true"`: veritabanları, SharedPreferences ve sarılmış anahtar bulut yedeğine gidebilir. `allowBackup="false"` yapın ve `dataExtractionRules` ile tüm alanları hariç tutun.

```xml
<application android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules" ... >

<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root"/><exclude domain="file"/>
        <exclude domain="database"/><exclude domain="sharedpref"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="root"/><exclude domain="file"/>
        <exclude domain="database"/><exclude domain="sharedpref"/>
    </device-transfer>
</data-extraction-rules>
```

### P1: kısa vadede

**3.8 Room veritabanı kalıntıları**

Android’in sistem SQLite’ı birçok sürümde secure_delete açık derlenir, ama bu garanti değildir (ilk değerlendirmede bunu kesin bir açık olarak ifade etmiştim; doğrusu “doğrulanmalı”). `PRAGMA secure_delete` değerini okuyun, kapalıysa açın; WAL için checkpoint(TRUNCATE), ardından VACUUM çalıştırın. Albüm şifrelendikten sonra mediaDB, directoryDB, favoriler ve dateTakens tablolarında o albümün yollarını temizleyin.

```kotlin
Room.databaseBuilder(ctx, GalleryDatabase::class.java, name)
    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.query("PRAGMA secure_delete = ON").close()
        }
    })
    .build()

fun scrubDatabase(db: SupportSQLiteDatabase) {
    db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
    db.execSQL("VACUUM")
}
```

**3.9 Glide ve diğer önbellekler**

`clearDiskCache` dosyaları yalnızca unlink eder. Önce klasördeki dosyaların üzerine yazın, sonra `clearDiskCache()` çağırın. Aynısı `externalCacheDir`, `removeTempFolder()` içeriği, widget önbelleği (`MyWidgetProvider`, RESOURCE) ve `PortraitPhotosAdapter` (RESOURCE) için geçerli. Gizli albümlerin widget’ta kapak olarak kullanılamadığını doğrulayın.

**3.10 Sanal albüm JSON dosyaları**

`Virtual Album Saves/*.json` albüm gizlenince düz `delete()` ile siliniyor; üzerine yazıp silin (küçük dosya, maliyet ihmal edilebilir). Gizli albüm dosya adı albüm adının özetinden türüyor (`fva_<hash>`): ad tahmininin doğrulanmasına izin verir. HMAC(MEK, ad) ya da rastgele kimlik kullanın. DOCS.md’deki 16 haneli hex örneği de gerçeğe uydurulmalı.

**3.11 SharedPreferences (Config)**

Yol taşıyan anahtarları denetleyin: sabitlenmiş, dahil edilen ve hariç tutulan klasörler, albüm kapakları, klasöre özel sıralama ve görünüm anahtarları, son açılan klasör. Albüm gizlenirken hepsini temizleyin. Not: SharedPreferences her yazımda dosyayı yeniden oluşturur ve eski blokları bırakır; burada ancak FBE korur. Bu yüzden hassas yolları en başta saklamamak en iyisidir.

**3.12 MediaStore ve sistem izleri**

`MediaStore.*.Thumbnails` tabloları API 29’dan beri kullanımdan kalktı; `purgeTracesForFile` içindeki bu temizlik muhtemelen etkisiz. Silmeden sonra üst klasörü tarayın (scanFile), satırların gittiğini doğrulayın, `.thumbnails` ve `.trashed-*` kalıntılarını arayın. Fossify’ın kendi geri dönüşüm kutusunda bu albümden gelen öğeler olabilir (denetleyin); varsa onları da ezin.

**3.13 Dosya zaman damgası ve boyut**

`encryptedFile.setLastModified(lastModified)` orijinal fotoğraf zamanını şifreli dosyaya taşıyor. Sabit veya rastgele zaman damgası kullanın; gerçek tarih zaten şifreli başlıkta. İsteğe bağlı: küçük dosyalarda boyut sınıflarına dolgu.

**3.14 Şifreleme formatı (v5)**

Mevcut yapıda dosya anahtarı doğrudan MEK; MAC oynatmada doğrulanmıyor (yalnızca dışa aktarma ve çözmede); başlık ile payload kriptografik olarak bağlı değil.

Öneri: Google Tink StreamingAead (AES-GCM-HKDF, 1 MiB segment; SeekableByteChannel ile rastgele erişim, yani ExoPlayer seek’i korunur) ve dosya başına rastgele anahtar (MEK ile sarılı, AAD = dosya kimliği). Her segment hem oynatmada hem dışa aktarmada doğrulanır. v1–v4 okunmaya devam eder, arka planda v5’e geçirilir.

Kendi CTR + Poly1305 kurgunuzu sürdürmek zorundaysanız: MAC’i en azından ilk açılışta doğrulayın, başlık özetini MAC girdisine katın, dosya başına anahtar türetin.

**3.15 Anahtar ve bellek hijyeni**

- Argon2 girdisi `String` yerine `CharArray` olsun; `secretBytes` ve `keyBytes` işlem sonunda sıfırlansın.
- `unlockedPrivateKey` nesne olarak bekliyor ve sıfırlanamıyor. PKCS8 baytlarını tutup gerektiğinde oluşturun ya da RSA’yı Android Keystore’a taşıyın (dışa aktarılamaz anahtar).
- `lockVault()` ile birlikte `VideoFragment.sVideoFrameCache` içindeki bitmap’leri `eraseColor(0)` + `recycle()` ile temizleyin ve `Glide.get(ctx).clearMemory()` çağırın. Şu an `clearFrameCache()` yalnızca ViewPagerActivity’den çağrılıyor ve `evictAll` pikselleri sıfırlamıyor.
- Bu en iyi çabadır: GC taşıması ve kopyalar yüzünden JVM’de kesin sıfırlama garanti edilemez. Dokümanda bunu belirtin.

**3.16 Kilit tetikleyicileri**

`onDestroy` ve gecikmeli handler’a güvenmeyin. Arka plana geçişte (kısa, ayarlanabilir tolerans) ve ekran kapanınca hemen kilitleyin.

```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = VaultSession.lockNow(appContext)
})
registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
```

**3.17 Diğer hatalar**

- `setupOrUpdateCredentials` kilitliyken çağrılırsa yeni MEK üretir ve mevcut dosyalar erişilemez olur; kilitliyken reddedin.
- `deleteExistingVault` başlığı ezmeden siliyor; sarılmış paket olsa da ezin.
- Kritik yollardaki `catch (_: Exception) {}` bloklarını kaldırın; detekt.yml’de EmptyCatchBlock ve SwallowedException kurallarını `helpers/` için hata seviyesine çekin.
- Logcat’e dosya adı veya yol yazan satırları tarayın.

## 4. Şifreleme sonrası temizlik hattı

Sıra önemlidir:

1. Ön kontrol: depolama dahili mi (SD/USB ise uyar), FBE durumu (`DevicePolicyManager.getStorageEncryptionStatus()` değeri ENCRYPTION_STATUS_ACTIVE_PER_USER mı), boş alan, şarj. Foreground service başlat.
2. Şifrele: geçici adla yaz (.tmp), `force`, ardından rename; düz metin SHA-256 özetini hesapla.
3. Doğrula: şifreli dosyayı çöz, özeti ve MAC’i karşılaştır. Biri başarısızsa tüm işlem geri alınır, orijinallere dokunulmaz.
4. Bekleyen-silme günlüğü: doğrulanmış düz metin yollarını uygulama özel dizinindeki bir dosyaya yazın (Room’a değil; anahtar gerekmez, çünkü doğrulama zaten yapıldı). Çökme veya süreç öldürülmesinde sonraki açılışta devam edilir.
5. Wipe: her dosya için 3.1’deki fonksiyon; başarılıları günlükten çıkarın, başarısızları kullanıcıya gösterin.
6. Uygulama izleri: DB satırları + checkpoint/VACUUM, favoriler, sanal albüm JSON, prefs anahtarları, Glide ve harici cache (üzerine yaz + temizle), geçici klasör, MediaStore satırları, geri dönüşüm kutusu.
7. Düz metin klasörünü silin; üst dizini fsync edin. Günlük dosyasını da ezip silin.
8. İkinci tur bellek temizliği: Glide bellek önbelleği ve frame cache.
9. Kullanıcıya özet: kaç dosya güvenle silindi, hangileri silinemedi, hangi iz temizlikleri yapıldı, önerilen sonraki adımlar.

## 5. Yeniden başlatma ve boşta bakım

- Uygulama cihazı programatik olarak yeniden başlatamaz; sonuç ekranında “Cihazı yeniden başlatın” önerisi gösterin.
- Önerilen kullanıcı rutini: albümü gizle, “güvenle silindi” raporunu gör, cihazı şarja tak ve ekranı kapatıp boşta bırak (idle maintenance çalışsın; mümkünse birkaç saat veya gece boyunca), ardından yeniden başlat.
- Yeniden başlatma sonrası ilk kilit açılışında (BOOT_COMPLETED; manifestte RECEIVE_BOOT_COMPLETED zaten var) bir WorkManager işi: bekleyen-silme günlüğünü kontrol et, kalan düz metinleri ez, DB checkpoint/VACUUM’u tekrarla, önbellek klasörlerini yeniden tara.
- Sıra: önce yeniden başlatmak RAM’i temizler ama discard’ı garanti etmez; önce boşta bekletmek discard şansını artırır. İkisi birbirini dışlamaz.

## 6. Ek sertleştirmeler

- Dışa aktarma, paylaşma, “birlikte aç”, yazdırma ve duvar kâğıdı akışlarında şifreli içeriğin düz metin geçici dosya üretmediğini test edin. Şifreli dosyanın olduğu gibi (şifreli haliyle) paylaşılması ayrı bir risktir; bu akışları vault dosyaları için kapatın veya açıkça yönetin.
- Bildirimlerde ve son kullanılanlar listelerinde albüm adı veya küçük resim göstermeyin.
- PIN/desen ekranlarını pencere örtüsü saldırılarına karşı `filterTouchesWhenObscured` ile koruyun.
- BouncyCastle ve Tink sürümlerini sabitleyin, CVE takibi yapın.
- Ağ yok: derlenmiş (merged) manifestte INTERNET izninin bulunmadığını CI’da doğrulayın.

## 7. Doğrulama planı

- Birim testleri: bilinen test vektörleri; bir bit çevir, doğrulama başarısız olmalı; kırpılmış dosya; başlık değişimi ve payload takası; yanlış parola; sınır dışı Argon2 parametreleri.
- Wipe mantığı testi: FBE’siz bir emülatör veya test imajında `CANARY-<uuid>` desenli 4 KB, 50 MB ve 500 MB dosyalar oluşturun, albümü gizleyin, userdata imajı dökümünde deseni arayın. FBE’li gerçek cihazda ham blok okunamayacağı için bu test wipe mantığını değil FBE’yi ölçer; ikisini karıştırmayın.
- Uygulama izi testi (debuggable derleme): `adb shell run-as <paket>` ile `databases/` (-wal ve -shm dahil), `shared_prefs/` ve `cache/` içinde deseni ve albüm adını arayın. `adb shell content query --uri content://media/external/file --where "_data LIKE '%AlbumAdi%'"` ile MediaStore satırlarını denetleyin; `/sdcard` altında `.thumbnails` ve `.trashed-*` arayın.
- Hata enjeksiyonu: salt-okunur dosya, kilitli dosya, düşük alan, işlem ortasında süreç öldürme, ekran kapanması, gelen çağrı.
- Arayüz: Recents’te albüm içeriğinin görünmediğini ve ekran görüntüsünün engellendiğini doğrulayın.
- Kod kalitesi: kritik paketlerde boş catch yasağı (detekt) ve lint.

## 8. Dokümantasyon düzeltmeleri (DOCS.md / FORK_CHANGELOG.md)

Önerilen ifadeler:

- Güvenli silme: “Orijinal dosyanın tüm içeriği üzerine yazılır, kısaltılır ve silinir. Flash depolamada fiziksel silme garanti edilemez; koruma esas olarak Android’in dosya tabanlı şifrelemesine (FBE) ve sistemin TRIM/discard işlemlerine dayanır.”
- RAM: “Anahtar malzemesi en iyi çabayla sıfırlanır; çözülmüş görüntüler bellekte kısa süre kalabilir. İşlem sonrası yeniden başlatma önerilir.”
- Diske yazma: “Uygulama çözülmüş içeriği kendi dosyalarına yazmaz; ekran görüntüsü, görev anlık görüntüsü ve sistem swap’ı uygulamanın kontrolü dışındadır (FLAG_SECURE ilk ikisini engeller).”
- “Multi-pass shredding”, “random + zero fill” ve “asla” ifadelerini kaldırın.

## 9. Öncelikli yol haritası

- Hafta 1 (P0): 3.1, 3.2, 3.4, 3.6, 3.7 ve dokümantasyon düzeltmeleri.
- Hafta 2 (P0/P1): 3.3 (Keystore bağlı sarma ve parola seçeneği), 3.5, 3.8–3.12, 3.16.
- Hafta 3 ve sonrası (P1/P2): 3.14 (v5 format ve geçiş), 3.15, 3.13, test altyapısı.

Süreler kabaca tahmindir. Format geçişi en riskli iştir ve geri dönüş planı (v1–v4 okuma desteği) gerektirir.

## 10. Kalan riskler

- Flash seviyesinde fiziksel silme garantisi yoktur; FBE ve TRIM’e dayanılır.
- Cihaz AFU iken ayrıcalıklı adli erişim kısmen mümkün olabilir.
- Orijinal dosyanın kamera, bulut ve mesajlaşma uygulamalarındaki kopyaları.
- Üretici katmanlarının (Samsung, Xiaomi vb.) kendi galeri, önbellek ve çöp kutusu mekanizmaları.
- Kullanıcı davranışı: zayıf PIN, ekran görüntüsü alma, başka uygulamaya paylaşma.