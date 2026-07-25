# ChessPlugin v4 — Tek Parca 8x8 Dunya-Uzayi Tahta (Spigot 26.2)

Bu surum, bir onceki surumde bildirdiginiz 5 sorunu da cozer:

## 1) Kendi kendine oynama

`/chess invite <kendi-kullanici-adiniz>` yazarsaniz oyun hemen baslar ve
tek basiniza hem Beyaz hem Siyah'i oynayabilirsiniz (sira otomatik
degisir, hangi renk oynuyorsaniz sizin tiklamalariniz o renk sayilir).

## 2) Mor/siyah (eksik doku) sorunu

Bunun sebebi bulundu: Minecraft'in yeni esya-bileseni sisteminde
(`item_model`), bir esyanin gorunumu artik DOGRUDAN bir modele degil,
once `assets/<namespace>/items/<isim>.json` adinda bir "esya tanimi"
dosyasina, o da bir modele isaret ediyor. Onceki kaynak paketinde bu ara
katman eksikti, bu yuzden istemci dokuyu bulamayip mor/siyah "eksik
doku" deseni gosteriyordu. Yeni kaynak paketinde hem Taslar kesesi hem
Satranc Masasi hem de tum tas ikonlari icin bu dosyalar eklendi.

## 3) Tek sayfada TAM 8x8 tahta

Vanilla Minecraft envanter GUI'leri **hicbir zaman** 6 satirdan (54 slot)
fazla olamaz — bu bir motor siniridir, genis sandik dahil hicbir vanilla
kap turu 8 satira sahip degildir. Bu yuzden envanter GUI'sinden tamamen
vazgecildi: **tahta artik bir envanter degil, dogrudan oyun dunyasinda
yuzen gercek boyutlu bir 8x8 izgaradir.** Her kare ve her tas ayri birer
gorsel varlik (ItemDisplay); tum 64 kare ayni anda, tek bakista gorunur.
Karelere doğrudan dunyada sag tiklayarak oynanir.

## 4) Gercek tas gorselleri

12 ayri, sifirdan cizilmis 2D tas ikonu eklendi (6 tas turu x 2 renk:
Beyaz/Siyah Sah, Vezir, Kale, Fil, At, Piyon). Chess.com gibi sitelerden
gorsel alinmadi (telif nedeniyle guvenli degil); bunun yerine benzer
sekilde taninabilir, ozgun silüetler cizildi. Artik Sah "elmas" veya
"nether yildizi" gibi alakasiz esyalar DEGIL, kendine ozgu bir sah
ikonu.

## 5) Rovans (rematch) ve "ayni yere koyamama" sorunu

- Oyun bitince (sah mat / pat / terk) tahtanin **saginda** donen bir ok
  ikonuyla "Rovans" butonu belirir. Ona sag tiklayan katilimci, rakibine
  rovans teklif eder; rakip cevrimiciyse ve `/chess accept` ile kabul
  ederse tahta sifirlanip yeni oyun baslar (renkler yer degistirir).
  Ayrica `/chess rematch` komutuyla da (tahtaya bakarken) ayni sey
  yapilabilir. Kendi kendine oynanan oyunlarda buton/komut direkt yeni
  oyunu baslatir (davet gerekmez).
- "Ayni yere tekrar koyamama" sorununun sebebi bulundu: bir tahta
  kirildiginda bazen kucuk gorsel varliklardan (ozellikle 8x8 izgaranin
  parcalari) biri tam olarak temizlenemiyor ve o noktada "hayalet" bir
  varlik kalip yeni yerlestirmeyi engelliyordu. Artik hem kirma sirasinda
  TUM izgara parcalari tek tek takip edilip temizleniyor, hem de her
  yeni yerlestirme girisiminde o bolgede baska "hayalet" bir varlik olup
  olmadigi once taranip temizleniyor. Sunucu yeniden baslatilip bellek
  sifirlansa bile (orn. restart sonrasi ilk etkilesimde) ayni temizlik
  otomatik calisir.

## Genel Akis (degismedi)

1. **Taslar kesesi**: 1 Kuvars Blogu + 1 Kayrak Tasi (sekilsiz tarif).
2. **Satranc Masasi**: 3x3 sekilli tarif (Kayrak-Kuvars-Kayrak /
   Kuvars-Kayrak-Kuvars / Tahta-Tahta-Tahta), bir bloğa sag tiklanarak
   yerlestirilir.
3. Tahtaya **Taslar** kesesiyle sag tiklayin (taslar yuklenir).
4. Tahtaya bakarken `/chess invite <oyuncu>` (rakip ya da kendiniz).
5. Rakip `/chess accept` ile katilir (kendi kendinize oynuyorsaniz bu
   adim atlanir, oyun direkt baslar).
6. Artik tahtanin ustunde beliren 8x8 izgaradaki karelere **dogrudan sag
   tiklayarak** oynayin — taş secme, hedef kareye tiklama, terfi (kucuk
   bir envanter menusunden secim — bu kucuk oldugu icin GUI olarak
   kaldi), sah/mat/pat, rok, en passant, coklu vezir terfisi hepsi
   calisir (motor degismedi, testli).
7. Kirma: masaya elinizle/kazmayla vurun (kazma seviyesine gore hizlanir,
   tablo asagida).

| Alet | Gereken vurus |
|---|---|
| Kazma yok / kazma degil | 20 |
| Tahta / Altin Kazma | 12 |
| Tas Kazma | 8 |
| Demir Kazma | 5 |
| Elmas Kazma | 3 |
| Netherite Kazma | 2 |

## Komutlar

| Komut | Aciklama |
|---|---|
| `/chess invite <oyuncu>` | Rakip davet eder. Kendi adinizi yazarsaniz kendi kendinize oynarsiniz. |
| `/chess accept` | Bekleyen daveti kabul edip Siyah olarak katilir. |
| `/chess decline` | Daveti reddeder. |
| `/chess resign` | Oyunu terk eder / bekleyen daveti iptal eder. |
| `/chess rematch` | Bitmis bir oyunda rovans teklif eder (tahtadaki butona da tiklayabilirsiniz). |
| `/chess help` | Yardim mesaji. |

## Proje Yapisi

```
ChessPlugin/
├── pom.xml
├── .github/workflows/build.yml   (GitHub Actions ile otomatik derleme)
├── BASIT_KURULUM.md               (hicbir sey kurmadan derleme rehberi)
├── src/main/resources/plugin.yml
└── src/main/java/com/chessplugin/
    ├── ChessPlugin.java
    ├── chess/                    (saf satranc motoru — degismedi, testli)
    ├── items/ChessItems.java     (Taslar + Satranc Masasi esyalari)
    ├── recipes/RecipeRegistrar.java
    ├── world/
    │   ├── ChessBoardEntity.java   (fiziksel "masa" varligi: yerlestirme/kirma/kalicilik)
    │   └── GridBoardRenderer.java  (YENI: tek parca 8x8 dunya-uzayi tahta + rovans butonu)
    ├── game/
    │   ├── BoardGame.java        (self-play, rovans, paylasimli secim durumu)
    │   ├── GameManager.java
    │   ├── GameStateCodec.java   (kalicilik: oyun durumunu metne kodlar/cozer)
    │   └── GameStatus.java
    ├── gui/                      (SADECE terfi menusu icin kucuk bir envanter GUI'si kaldi)
    ├── listeners/
    │   ├── ChessEntityListener.java  (yerlestirme, tum sag-tik turleri, kirma)
    │   └── ChessGuiListener.java     (sadece terfi menusu)
    └── commands/ChessCommand.java
```

Tum proje (chess motoru dahil) sifir hatayla derlendi ve motor testleri
(baslangicta 20 hamle, Fool's Mate mati, tas alma, en passant, rok,
coklu vezir terfisi) gecti.

## Kaynak Paketini Kurma

`ChessBoardResourcePack.zip` (yeni surum) ayri bir dosya olarak verildi;
sadece `chessplugin:` namespace'i icerir, hicbir vanilla dosyaya
dokunmaz. Kurulum talimatlari onceki mesajlarda anlatildigi gibi
(server.properties `resource-pack=` / `resource-pack-sha1=` ile zorunlu
kilinmasi onerilir).

**Onemli**: Paket kurulu olmayan oyuncular icin tum ozel gorunumler
(masa, kareler, taslar, rovans butonu) `Material.PAPER` varsayilan
gorunumunde (duz kagit) gozukur — islevsellik etkilenmez, sadece gorsel
farkli olur.

## Bilinen Sinirlamalar (guncel)

- Grid'in her karesi/tasi/butonu ayri birer varlik oldugu icin, aktif
  bir oyun basina ~130-160 varlik olusabilir (64 kare + en fazla 32 tas
  + 64 tiklama kutusu + rovans butonu). Ayni anda cok sayida (10+) aktif
  oyun beklemiyorsaniz bu performans acisindan sorun olusturmaz.
- Boyut/aci (olcek, donuklük) degerleri gercek bir Minecraft istemcisi
  uzerinde test edilemedigi icin (bu ortamda calistirilamiyor) en iyi
  tahminle ayarlandi; gorunum biraz kucuk/buyuk ya da hafif yanlis acili
  gorunurse bana soyleyin, `GridBoardRenderer.java` icindeki `CELL`,
  `SQUARE_Y`, `PIECE_Y` sabitlerini ve donus acisini birlikte ince
  ayarlariz.
- Satranc Masasinin gercek fiziksel collision'i (yuruyerek gecilememesi)
  yoktur.
- Bir oyuncu ayni anda yalnizca bir oyunda/beklemede olabilir.
- Ucler kurali / 50 hamle beraberligi ve hamle geri alma kapsam disidir.
- Rok/en passant haklari, sunucu tam bir oyun ortasinda yeniden
  baslatilirsa nadir durumlarda sifirlanabilir (kalicilik kodlamasi
  basit tutuldu).

## Derleme

`BASIT_KURULUM.md` dosyasindaki adimlari izleyin (hicbir sey kurmadan,
sadece GitHub uzerinden derleme). Ozet: GitHub'a yukleyin, Actions
sekmesinde otomatik derlemeyi bekleyin, cikan `ChessPlugin.jar`'i
indirip exaroton'a atin.
