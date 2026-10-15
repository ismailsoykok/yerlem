[![Architecture diagram of ismailsoykok/yerlem](https://gitdiagram.com/ismailsoykok/yerlem/diagram.png)](https://gitdiagram.com/ismailsoykok/yerlem?utm_source=readme&utm_medium=picture)

# Yerlem Backend — Detaylı Dokümantasyon

Kısa mesafeli araç kiralama (elektrikli araç paylaşımı) senaryosu için yazılmış
Spring Boot REST API'si. Kullanıcı kaydı/OTP doğrulaması, cüzdan/ödeme, araç
listeleme, seyahat tahmini başlatma, konum akışı, ücretlendirme, uyarı (alert)
üretimi, arıza bildirimi ve **bonus olarak fotoğraf yükleme + mock hasar tespiti**
özelliklerini içerir.

Bu doküman; sistemin nasıl çalıştığını, hangi servisin hangi işi yaptığını,
uçtan uca bir test senaryosunun nasıl koşulacağını, build/migration süreçlerini
ve bilinen kısıtları anlatır.

---

## 1. Genel Bakış

Sistemin ana akışı:

1. Kullanıcı **kayıt** olur (PENDING durumunda), sistem 6 haneli OTP üretir.
2. Kullanıcı **OTP doğrulaması** yapar → hesap ACTIVE olur ve JWT token alır.
3. Kullanıcı **cüzdanına bakiye yükler** (mock ödeme sağlayıcısı).
4. **Araçları listeler**, bir araç seçer.
5. Hedef için **tahmini ücret/süre** alır (`/trips/estimate`).
6. **Seyahati başlatır** (`POST /trips`); bakiye kontrolü yapılır, araç IN_USE olur.
7. Seyahat sırasında **konum gönderir** (`/trips/{id}/locations`); hız ihlali ve
   menzil kontrolü yapılır, gerekirse uyarı üretilir.
8. Fotoğraf yüklemek isterse **başlangıç/bitiş fotoğrafı** ekler ve
   **mock hasar tespiti** çalıştırır (bonus).
9. **Seyahati tamamlar** (`/trips/{id}/complete`); gerçek mesafe/süre hesaplanır,
   cüzdandan ücret tahsil edilir, araç AVAILABLE olur.
10. Gerektiğinde seyahate bağlı **arıza** bildirir ve **uyarıları** listeler.

Her modül bağımsız bir paket olarak yazılmıştır; controller → service →
repository/entity katmanları izlenir.

---

## 2. Teknoloji Yığını

| Bileşen | Sürüm / Not |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Spring Web | REST API |
| Spring Data JPA | Hibernate ORM |
| Spring Security | JWT tabanlı, stateless |
| Spring Validation | Bean Validation (`@Valid`) |
| PostgreSQL | `localhost:5433/backend_db` |
| PostGIS | Mesafe hesapları (`ST_Distance`, `geography`) |
| Flyway | Veritabanı migration yönetimi |
| JJWT | `0.12.6` (API + impl + jackson) |
| Maven Wrapper | `mvnw.cmd` (Windows) / `mvnw` (Unix) |
| Test | JUnit 5 + Mockito + AssertJ |

Not: PostGIS fonksiyonları kullanıldığı için veritabanında `postgis` eklentisinin
kurulu/aktif olması gerekir. Migration'lar bunu kurmaz; ortamda hazır kabul edilir.

---

## 3. Mimari ve Katmanlar

```
client (Postman / mobil)
        │  HTTP + JWT (Authorization: Bearer <token>)
        ▼
JwtAuthenticationFilter  →  SecurityContext'e userId koyar
        ▼
Controller (@RestController)  →  istek/yanıt DTO'ları
        ▼
Service (@Service, @Transactional)  →  iş kuralları
        ▼
Repository (JpaRepository / native query)  →  veri erişimi
        ▼
PostgreSQL + PostGIS
```

- **Controller**: Sadece HTTP katmanı, doğrulama ve servis çağrısı.
- **Service**: İş kuralları; transaction sınırları; hata durumları
  `ResponseStatusException` ile fırlatılır.
- **Repository**: Spring Data türetilmiş sorgular + gerekli yerlerde native SQL
  (PostGIS) ve pessimistic lock.
- **Entity**: Tablo eşlemeleri; `@PrePersist`/`@PreUpdate` ile zaman damgaları.

Hata yönetimi merkezîdir: `common/exception/GlobalExceptionHandler`
(`@RestControllerAdvice`) `ResponseStatusException`, `IllegalArgumentException`
ve bozuk JSON gövdesini standart bir JSON hataya çevirir.

---

## 4. Paket/Dizin Yapısı

```
src/main/java/com/example/backend
├── BackendApplication.java
├── alert/                 # Seyahat uyarıları (hız, süre, arıza)
│   ├── controller?        (uyarılar TripController üzerinden)
│   ├── dto/TripAlertResponse.java
│   ├── entity/TripAlert.java, TripAlertEntity.java
│   ├── repository/TripAlertRepository.java
│   └── service/AlertService.java
├── common/exception/GlobalExceptionHandler.java
├── config/                # SecurityConfig, PasswordConfig
├── fare/                  # Şehir tarifesi + seyahat ücreti
│   ├── entity/CityFare.java, TripFare.java, TripFareStatus.java
│   └── repository/CityFareRepository.java, TripFareRepository.java
├── fault/                 # Arıza bildirimi
│   ├── controller/FaultController.java
│   ├── dto/FaultReportRequest.java, FaultReportResponse.java
│   ├── entity/FaultReport.java, FaultStatus.java, FaultType.java
│   ├── repository/FaultReportRepository.java
│   └── service/FaultService.java
├── photo/                 # (BONUS) Fotoğraf + mock hasar tespiti
│   ├── controller/TripPhotoController.java
│   ├── dto/TripPhotoRequest.java, TripPhotoResponse.java,
│   │      DamageDetectionResult.java, DamageChargeResponse.java,
│   │      DamageDetectionResponse.java
│   ├── entity/PhotoType.java, PhotoStage.java, TripPhoto.java,
│   │        DamageCharge.java
│   ├── repository/TripPhotoRepository.java, DamageChargeRepository.java
│   └── service/TripPhotoService.java, MockDamageDetectionService.java
├── security/              # JwtAuthenticationFilter, CurrentUserService
├── trip/                  # Seyahat çekirdeği
│   ├── controller/TripController.java
│   ├── dto/ (TripCreate/Complete/Estimate/Location Request-Response)
│   ├── entity/Trip.java, TripLocation.java, TripStatus.java
│   ├── repository/TripRepository, TripLocationRepository,
│   │             PostgisDistanceRepository, PostgisTripDistanceRepository
│   └── service/TripService.java
├── user/                  # Kayıt, OTP, login
│   ├── controller/UserController.java
│   ├── dto/AuthResponse, LoginRequest, UserRegisterRequest, VerifyOtpRequest
│   ├── entity/User.java, UserStatus.java, OtpCode.java
│   ├── repository/UserRepository.java, OtpCodeRepository.java
│   └── service/UserService, JwtService, SmsService, MockSmsService
├── vehicle/               # Araç listeleme
│   ├── controller/VehicleController.java
│   ├── dto/VehicleResponse.java
│   ├── entity/Vehicle.java, VehicleStatus.java, VehicleUsageArea.java
│   ├── repository/VehicleRepository.java, VehicleUsageAreaRepository.java
│   └── service/VehicleService.java
└── wallet/                # Cüzdan + ödeme
    ├── controller/WalletController.java
    ├── dto/WalletTopUpRequest.java, WalletTopUpResponse.java
    ├── entity/Wallet.java, WalletMovement.java, WalletMovementType.java
    ├── repository/WalletRepository.java, WalletMovementRepository.java
    └── service/WalletService.java, payment/PaymentProvider,
        PaymentResult, TestPaymentProvider

src/main/resources
├── application.properties
└── db/migration/V1..V6  (Flyway)

src/test/java/com/example/backend   # Birim testleri (Mockito)
```

---

## 5. Konfigürasyon (`application.properties`)

```properties
spring.application.name=backend
spring.datasource.url=jdbc:postgresql://localhost:5433/backend_db
spring.datasource.username=postgres
spring.datasource.password=ismail42
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate   # Şema Flyway'den gelir, JPA sadece doğrular
spring.jpa.show-sql=true

spring.flyway.enabled=true

server.port=8082

app.jwt.secret=yerel-gelistirme-icin-en-az-32-karakterlik-gizli-anahtar
app.jwt.expiration-seconds=86400          # 24 saat
```

Önemli noktalar:

- `ddl-auto=validate`: Entity ile migration şeması **bire bir** uyuşmalıdır.
  Yeni kolon/tablo eklerseniz mutlaka yeni bir migration yazmalısınız.
- JWT gizli anahtarı en az 32 karakter olmalı (HS256).
- Uygulama **8082** portunda çalışır.

---

## 6. Veritabanı ve Flyway Migration'ları

Şema tamamen Flyway ile yönetilir. Migration dosyaları
`src/main/resources/db/migration` altında `V{num}__{açıklama}.sql` şeklindedir.
Uygulama açılışında sırayla ve **yalnızca bir kez** uygulanır; uygulananlar
`flyway_schema_history` tablosuna kaydedilir.

| Sürüm | Dosya | İçerik |
|---|---|---|
| V1 | `V1__create_tables.sql` | Tüm ana tablolar (users, otp_codes, vehicles, vehicle_usage_areas, city_fares, trips, trip_locations, trip_fares, wallets, wallet_movements, fault_reports, trip_alerts) + index/constraint |
| V2 | `V2__seed_data.sql` | 3 şehir tarifesi, 5 doğrulanmış kullanıcı, 3 araç, kullanım alanları, 500 TRY cüzdan |
| V3 | `V3__add_password_hash_to_users.sql` | `users.password_hash` kolonu |
| V4 | `V4__add_password_to_seed_users.sql` | Seed kullanıcılara şifre hash'i |
| V5 | `V5__add_vehicles_data.sql` | Ek test araçları (MAINTENANCE/IN_USE/düşük menzil dahil) |
| V6 | `V6__add_trip_photos_and_damage_charges.sql` | (BONUS) `trip_photos` ve `trip_damage_charges` tabloları |

### Yeni migration ekleme kuralları

1. Yeni dosyayı **bir sonraki numarayla** oluştur: `V7__aciklama.sql`.
2. **Uygulanmış migration'ları asla değiştirme** (Flyway checksum uyuşmazlığı
   verir ve uygulama başlamaz). Değişiklik gerekiyorsa yeni migration yaz.
3. Yeni bir entity ekliyorsan, kolon adlarını/tip/nullable değerlerini
   migration ile bire bir eşleştir (`ddl-auto=validate` yüzünden).
4. UUID için `gen_random_uuid()` kullanılıyor (PostgreSQL 13+ yerleşik).

### Önemli kısıtlar (iş kurallarını veritabanı da destekler)

- `uq_trips_active_vehicle`: Bir araç için aynı anda **tek** `IN_PROGRESS`
  seyahat olabilir (partial unique index).
- `wallet_movements`: `TOP_UP`, `TRIP_CHARGE`, `REFUND`, `ADJUSTMENT`
  ve `idempotency_key` UNIQUE.
- `uq_wallet_movement_trip_charge`: Aynı seyahat için tek `TRIP_CHARGE`
  hareketi (çift tahsilatı engeller).
- `vehicles`: `remaining_range_km <= total_range_km`, negatif değer yok.
- `trips`: `completed_at >= started_at`, koordinat aralık kontrolleri.

### Seed veriler (V2 + V5)

**Şehir tarifeleri:** Ankara 10 TRY/km, Istanbul 15 TRY/km, Izmir 12 TRY/km.

**Kullanıcılar:** `5550000001` … `5550000005`, tümü ACTIVE ve telefonu doğrulanmış.
Seed kullanıcıların şifresi: **`123456789`**. Her birinin cüzdanı 500 TRY.

**Araçlar:**

| Plaka | Marka/Model | Şehir | Kalan Menzil | Durum |
|---|---|---|---|---|
| 06 TEST 001 | Renault Zoe | Ankara | 280 | AVAILABLE |
| 34 TEST 002 | Togg T10X | Istanbul | 350 | AVAILABLE |
| 35 TEST 003 | Fiat 500e | Izmir | 220 | AVAILABLE |
| 06 TEST 004 | Nissan Leaf | Ankara | 240 | AVAILABLE |
| 34 TEST 005 | Tesla Model Y | Istanbul (+Ankara) | 410 | AVAILABLE |
| 35 TEST 006 | Fiat 500e | Izmir | 20 | AVAILABLE (menzil testi) |
| 06 TEST 007 | Renault Megane E-Tech | Ankara | 300 | MAINTENANCE |
| 34 TEST 008 | Togg T10X | Istanbul | 320 | IN_USE |
| 06 TEST 009 | Tesla Model 3 | Ankara (+Istanbul) | 470 | AVAILABLE |

---

## 7. Kimlik Doğrulama ve Güvenlik

### Kayıt / Doğrulama / Giriş akışı (`user` modülü)

- **`UserService.register`**: Telefon ve kimlik numarası benzersizliğini
  doğrular, şifreyi **BCrypt** ile hashler, kullanıcıyı `PENDING` olarak kaydeder,
  6 haneli OTP üretir (5 dk geçerli) ve hash'ini `otp_codes`'a yazar.
  OTP **log'a yazılır** (`[MANUEL TEST] Telefon: ..., OTP: ...`) — gerçek SMS yok.
- **`UserService.verifyOtp`**: Telefon + OTP kontrolü, son kullanma süresi ve tek
  kullanımlık tüketim kontrolü. Başarılıysa `phone_verified=true`, durum `ACTIVE`,
  ve JWT token üretilir.
- **`UserService.login`**: Telefon + şifre (BCrypt.match) kontrolü; aktif ve
  doğrulanmış kullanıcıya JWT döner. Girişte OTP üretilmez.

`AuthResponse` = `accessToken`, `expiresIn`, `user{id, fullName, phone, status}`.

### JWT

- **`JwtService.generateToken`**: `sub` = userId, `phone` claim, `iat/exp`.
- **`JwtService.extractUserId`**: İmza doğrulaması sonrası `sub`'ı UUID'ye çevirir.
- **`JwtAuthenticationFilter`** (`OncePerRequestFilter`): `Authorization: Bearer`
  başlığını okur, geçerliyse `SecurityContext`'e userId'yi principal olarak koyar.
  Geçersiz/süresi dolmuş token → **401**.
- **`CurrentUserService.getUserId`**: Aktif isteğin kullanıcı ID'sini verir;
  yoksa `IllegalStateException`.

### Güvenlik politikası (`SecurityConfig`)

- CSRF, form-login, http-basic kapalı; oturum **stateless**.
- Herkese açık: `POST /users/register`, `/users/verify-otp`, `/users/login`.
- Diğer **tüm** endpoint'ler `authenticated()` → geçerli JWT zorunlu.
- `PasswordConfig` BCrypt encoder bean'i sağlar.

---

## 8. Modüller ve Servisler — Kim Ne Yapar?

### 8.1 `vehicle` — Araç listeleme
- **`VehicleController` / `VehicleService`**: `GET /vehicles` ve opsiyonel
  `?status=AVAILABLE|IN_USE|MAINTENANCE` filtresi. `VehicleResponse` döner
  (id, make, model, currentCity, remainingRangeKm, konum).
- **`VehicleRepository`**: `findAllByStatus`, ve seyahat başlatmada kullanılan
  kilitlemeli `findByIdForUpdate` (PESSIMISTIC_WRITE).
- **`VehicleUsageAreaRepository`**: Aracın hizmet verdiği şehirleri tutar;
  `existsByVehicleIdAndCityNameIgnoreCase` ile hedef şehir kontrolü.

### 8.2 `fare` — Tarife ve ücret
- **`CityFare`**: Şehir başına TRY/km.
- **`TripFare`**: Seyahat başına ücret kaydı (distance, pricePerKm, totalAmount,
  currency, status: `ESTIMATED` → `FINALIZED`/`FAILED`).
- **`CityFareRepository.findByCityNameAndActiveTrue`**, **`TripFareRepository.findByTripId`**.

### 8.3 `trip` — Seyahat çekirdeği (`TripService`)
- **`estimateTrip`**: Araç/koordinat/hedef şehir doğrulaması; araç AVAILABLE mı,
  menzil/konum geçerli mi, hedef şehir aracın kullanım alanında mı; aktif şehir
  tarifesi var mı. PostGIS ile mesafe (km), ~50 km/s varsayımıyla süre, ücret
  hesabı. **DB'ye yazmaz.**
- **`createTrip`**: Kullanıcı ACTIVE + telefon doğrulanmış olmalı. Aracı kilitler,
  ücreti **sunucuda yeniden hesaplar** (client'a güvenmez), bakiye yeterliliğini
  kontrol eder, `Trip` (IN_PROGRESS) ve `TripFare` (ESTIMATED) oluşturur, aracı
  IN_USE yapar.
- **`saveTripLocation`**: Sahiplik + IN_PROGRESS kontrolü; konumu kaydeder.
  - Şehir içi (origin ile aynı) hız limiti **80 km/s**, şehir dışı **120 km/s**
    aşılırsa `SPEED_LIMIT_EXCEEDED` uyarısı.
  - Kalan menzil 1 km altına düşerse **otomatik `completeTrip`** çağrılır.
- **`completeTrip`**: Seyahati kilitler; sahiplik + IN_PROGRESS kontrolü; son
  konumu kaydeder; hız ihlali uyarısı; **PostGIS ile gerçek mesafe** (noktalar
  arası toplam); gerçek süre; süre tahminin %20 üzerindeyse `DURATION_EXCEEDED`.
  `TripFare`'i `FINALIZED` yapar, **cüzdandan tahsilat**, aracın konumu/menzili
  güncellenir, araç (MAINTENANCE değilse) AVAILABLE olur; özet `TripCompleteResponse`
  döner.
- **PostGIS repository'leri**: `PostgisDistanceRepository.calculateDistanceKm`
  (iki nokta arası), `PostgisTripDistanceRepository.calculateActualDistanceKm`
  (origin + tüm location noktalarının sıralı toplamı).

### 8.4 `alert` — Seyahat uyarıları
- **`AlertService.createAlert`**: Seyahate bağlı uyarı kaydı oluşturur.
- **`AlertService.getTripAlerts`**: `GET /trips/{id}/alerts`, en yeniden eskiye.
- Tipler: `SPEED_LIMIT_EXCEEDED`, `DURATION_EXCEEDED`, `FAULT_REPORTED`, `OTHER`.

### 8.5 `fault` — Arıza bildirimi
- **`FaultService.reportFault`**: Sahiplik + IN_PROGRESS kontrolü; `FaultReport`
  kaydeder, aracı **MAINTENANCE** yapar, `FAULT_REPORTED` uyarısı üretir.
  Tipler: `TIRE`, `BATTERY`, `DOOR`, `MOTOR`, `OTHER`.
- **`FaultService.getTripFaults`**: Seyahatin arızalarını listeler.

### 8.6 `wallet` — Cüzdan ve ödeme
- **`WalletService.checkSufficientBalance`**: Bakiye yeterli mi (read-only).
- **`WalletService.chargeTrip`**: Seyahat ücretini kilitleyerek tahsil eder;
  `idempotency_key = "TRIP_CHARGE:<tripId>"` sayesinde **çift tahsilatı engeller**.
- **`WalletService.topUp`**: `TestPaymentProvider` üzerinden (her zaman başarılı)
  bakiye yükler; `idempotency_key = "TOP_UP:<providerRef>"`.
- **`TestPaymentProvider`**: Gerçek ödeme yok, daima `successful=true` ve
  `TEST-<uuid>` referansı döner.

### 8.7 `photo` — (BONUS) Fotoğraf + mock hasar tespiti
- **`TripPhotoService.uploadPhoto`**: Seyahate başlangıç/bitiş fotoğrafı ekler.
  `photoType` = `FRONT_SEAT` | `BACK_SEAT` | `EXTERIOR`, `stage` = `START` | `END`,
  `photoUrl` serbest metin (dosya yolu/URL/mock). Sahiplik ve seyahat durumu
  (IN_PROGRESS veya COMPLETED) kontrol edilir. Gerçek dosya depolama YOK.
- **`TripPhotoService.getPhotos`**: Seyahatin fotoğraflarını listeler.
- **`TripPhotoService.detectDamage`**: Bölge bazında en son START ve END
  fotoğrafını eşleştirir; `MockDamageDetectionService` ile hasar olup olmadığını
  belirler. Tespit edilen her bölge için `DamageCharge` (ayrı ücret kalemi)
  oluşturur. Aynı bölge için ikinci kez ücret eklenmez.
- **`MockDamageDetectionService`**: Gerçek görüntü işleme/AI yerine basit
  anahtar kelime kontrolü. END fotoğrafının adresinde şu ifadelerden biri
  geçiyorsa hasar var sayılır: `damage, scratch, crack, dent, broken, hasar,
  cizik, çizik, kirik, kırık, ezik`.
- **Ücret kalemleri (sabit):** EXTERIOR 1000 TRY, FRONT_SEAT 500 TRY,
  BACK_SEAT 300 TRY.
- Not: Bu ek ücret, mevcut `TripFare` toplamına karışmaz ve cüzdandan otomatik
  tahsil edilmez; sadece ayrı bir kayıt olarak tutulur.

---

## 9. Uç Noktalar (API)

`baseUrl = http://localhost:8082`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/users/register` | Public | Kayıt + OTP üretimi |
| POST | `/users/verify-otp` | Public | OTP doğrulama → JWT |
| POST | `/users/login` | Public | Giriş → JWT |
| POST | `/wallet/top-up` | JWT | Cüzdana bakiye yükleme |
| GET | `/vehicles?status=` | JWT | Araç listesi (filtre opsiyonel) |
| POST | `/trips/estimate` | JWT | Tahmini mesafe/süre/ücret |
| POST | `/trips` | JWT | Seyahat başlat |
| POST | `/trips/{tripId}/locations` | JWT | Konum gönder |
| POST | `/trips/{tripId}/complete` | JWT | Seyahati tamamla |
| GET | `/trips/{tripId}/alerts` | JWT | Uyarıları listele |
| POST | `/trips/{tripId}/faults` | JWT | Arıza bildir |
| GET | `/trips/{tripId}/faults` | JWT | Arızaları listele |
| POST | `/trips/{tripId}/photos` | JWT | Fotoğraf yükle (bonus) |
| GET | `/trips/{tripId}/photos` | JWT | Fotoğrafları listele (bonus) |
| POST | `/trips/{tripId}/damage-detection` | JWT | Mock hasar tespiti (bonus) |

Yetkili isteklerde başlık: `Authorization: Bearer <accessToken>`.

### Örnek gövdeler

**Kullanıcı kaydı**
```json
{ "fullName": "Ahmet Yılmaz", "phone": "5321234567",
  "password": "Test1234", "identityNumber": "12345678903" }
```

**OTP doğrulama / login**
```json
{ "phone": "5550000001", "code": "123456" }     // verify-otp
{ "phone": "5550000001", "password": "123456789" } // login
```

**Cüzdan yükleme**
```json
{ "amount": 1000.00 }
```

**Seyahat tahmini / başlatma**
```json
{ "vehicleId": "<UUID>", "destinationCity": "Ankara",
  "destinationLatitude": 39.9208, "destinationLongitude": 32.8541 }
```

**Konum / tamamlama**
```json
{ "latitude": 39.9250, "longitude": 32.8600,
  "cityName": "Ankara", "speedKmh": 65 }
```

**Fotoğraf yükleme (bonus)**
```json
{ "photoType": "EXTERIOR", "stage": "START", "photoUrl": "mock://on-koltuk-temiz.jpg" }
{ "photoType": "EXTERIOR", "stage": "END",   "photoUrl": "mock://on-koltuk-scratch.jpg" }
```

**Hasar tespiti (bonus):** Gövde yok. `POST /trips/{tripId}/damage-detection`
çağrıldığında tespit çalışır ve örnek yanıt döner:
```json
{
  "tripId": "<UUID>",
  "damageCharges": [
    { "id": "<UUID>", "photoType": "EXTERIOR",
      "description": "EXTERIOR bölgesinde hasar tespit edildi (mock tespit).",
      "amount": 1000.00, "currency": "TRY", "createdAt": "..." }
  ],
  "totalExtraAmount": 1000.00,
  "currency": "TRY"
}
```

---

## 10. Uçtan Uca Test / Kullanım Senaryosu

### Ön koşullar
- PostgreSQL (5433) çalışıyor ve `backend_db` veritabanı mevcut, PostGIS aktif.
- Uygulama çalışıyor: `http://localhost:8082`.
- Postman koleksiyonu: `Postman/Yerlem.postman_collection.json`
  (environment: `Postman/Enviroment.postman_environment.json`).
  Environment değişkenleri: `baseUrl`, `Bearer`, `tripID`.

### Senaryo A — Seed kullanıcı ile hızlı akış
1. **Login** (`POST /users/login`) → `5550000001` / `123456789`.
   Yanıttan `accessToken`'ı `Bearer` olarak kaydet.
2. **Araçları listele**: `GET /vehicles?status=AVAILABLE`.
   Ankara aracı `06 TEST 001`'in UUID'sini al.
3. **Tahmin**: `POST /trips/estimate` (destination `Ankara`, 39.9208 / 32.8541).
   Dönen `distanceKm`, `estimatedDurationMinutes`, `estimatedFee` (≈ mesafe x 10).
4. **Seyahat başlat**: `POST /trips` (aynı gövde).
   → `tripId` (`tripID` değişkenine kaydet). Araç IN_USE olur.
5. **Konum gönder**: `POST /trips/{tripId}/locations` birkaç nokta.
   - `speedKmh: 95` + `cityName: "Ankara"` → `SPEED_LIMIT_EXCEEDED` uyarısı.
6. **Fotoğraf yükle (bonus)**:
   - START `EXTERIOR` `mock://on-temiz.jpg`
   - END `EXTERIOR` `mock://on-scratch.jpg`
7. **Hasar tespiti (bonus)**: `POST /trips/{tripId}/damage-detection`
   → EXTERIOR için 1000 TRY ek ücret kalemi.
8. **Tamamla**: `POST /trips/{tripId}/complete` (son konum).
   → gerçek mesafe/süre, tahsilat, araç AVAILABLE.
9. **Uyarılar**: `GET /trips/{tripId}/alerts`.
10. **Arıza**: `POST /trips/{tripId}/faults` (seyahat IN_PROGRESS iken) →
    araç MAINTENANCE.

### Senaryo B — Sıfırdan kayıt
1. `POST /users/register` (yeni telefon) → 201, OTP **log'da** görünür.
2. Log'dan 6 haneli OTP'yi al: `POST /users/verify-otp` → JWT.
3. `POST /wallet/top-up` ile bakiye yükle (yeni kullanıcının cüzdanı seed'de
   oluşturulmaz; seed dışı kullanıcılar için cüzdan akışı ayrıca ele alınmalıdır,
   bkz. Bölüm 13).
4. Araç seç → tahmin → başlat → konum → tamamla.

### Beklenen sonuçlar / iş kuralları
- Hedef şehir aracın kullanım alanı dışındaysa **400**.
- Mesafe kalan menzili aşıyorsa **400**.
- Bakiye yetersizse seyahat başlatma/tamamlama **409**.
- Aynı araca ikinci eşzamanlı IN_PROGRESS seyahat **409/unique ihlali**.
- Şehir içi >80 veya şehir dışı >120 km/s → uyarı kaydı.
- Süre, tahminin %20 üzerinde → `DURATION_EXCEEDED`.
- Kalan menzil <1 km → konum gönderiminde otomatik tamamlama.

---

## 11. Mock Sistemler (Gerçek Olmayan Bileşenler)

| Bileşen | Davranış |
|---|---|
| OTP/SMS | Gerçek SMS yok; doğrulama kodu uygulama log'una yazılır |
| `MockSmsService` | `@Profile("dev")`; şu an UserService doğrudan logluyor |
| `TestPaymentProvider` | Ödeme daima başarılı, referans `TEST-<uuid>` |
| Mesafe | PostGIS `ST_Distance` (gerçek hesap) |
| Hasar tespiti | `MockDamageDetectionService`: URL'deki anahtar kelimeye göre tespit |

---

## 12. Build ve Çalıştırma

### Gereksinimler
- JDK 21
- PostgreSQL + PostGIS (5433)
- İnternet/yerel Maven deposu (bağımlılıklar için)

### Veritabanı hazırlığı
```sql
CREATE DATABASE backend_db;
CREATE EXTENSION IF NOT EXISTS postgis;   -- PostGIS gerekli
-- Kullanıcı: postgres / şifre: ismail42 (application.properties ile uyumlu)
```

### Derleme
```powershell
# Windows
.\mvnw.cmd clean package

# Unix/macOS
./mvnw clean package
```

### Çalıştırma
```powershell
# Doğrudan çalıştır
.\mvnw.cmd spring-boot:run

# veya üretilen jar
.\mvnw.cmd clean package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Uygulama açılışta **Flyway**'i çalıştırır (V1..V6) ve ardından Hibernate
`validate` yapar. Şema veya seed veri gerekiyorsa migration dosyaları ile yönetilir;
manuel SQL çalıştırmaya gerek yoktur.

---

## 13. Testler

Birim testleri Mockito ile bağımsız (DB gerektirmez). Konum:
`src/test/java/com/example/backend`.

| Test sınıfı | Kapsam |
|---|---|
| `UserServiceTest` | Kayıt, OTP, login, hash'leme |
| `JwtServiceTest` | Token üret/çöz, süre |
| `MockSmsServiceTest` | Mock SMS |
| `CurrentUserServiceTest` | Aktif kullanıcı çözümü |
| `VehicleServiceTest` | Araç listeleme/filtre |
| `WalletServiceTest` | Bakiye, tahsilat, idempotency |
| `TestPaymentProviderTest` | Mock ödeme |
| `TripServiceTest` | Tahmin/başlat/konum/tamamla kuralları |
| `AlertServiceTest` | Uyarı oluştur/listele |
| `FaultServiceTest` | Arıza bildir/ listele |
| `TripPhotoServiceTest` | Fotoğraf yükleme + mock hasar tespiti (bonus) |
| `BackendApplicationTests` | `contextLoads` — **çalışan DB gerektirir** |

### Testleri çalıştırma
```powershell
# Tümü (Not: contextLoads çalışan DB ister)
.\mvnw.cmd test

# Belirli bir sınıf
.\mvnw.cmd -Dtest=TripPhotoServiceTest test

# contextLoads'u hariç tutarak hızlı birim testleri
.\mvnw.cmd test "-Dtest=!BackendApplicationTests"
```

Not: `BackendApplicationTests` `@SpringBootTest` olduğu için gerçek datasource +
Flyway başlatır. DB yoksa bu test hata verir; diğer birim testleri etkilenmez.

---

## 14. Postman Koleksiyonu

`Postman/Yerlem.postman_collection.json` içinde akış klasörleri:

`Register → Otp Verify → Login → Wallet TopUp → Vehicles → Estimate (Ankara/
Izmir/Istanbul) → Start Trip → Trip Locations → Complete Trip → Alerts → Fault`.

- `Login` isteğinin test script'i `accessToken`'ı environment'taki `Bearer`
  değişkenine yazar.
- Seyahat başlatma yanıtından gelen `tripId`, `tripID` değişkenine yazılır.
- Environment'ta `baseUrl`'i `http://localhost:8082` olarak ayarlayın.



---

## 15. Bilinen Kısıtlar / Yapılmayanlar

- Fotoğraf **depolama yoktur**: sadece `photoUrl` metni saklanır (bonus kapsamı).
- Hasar tespiti gerçek görüntü işleme/AI içermez; anahtar kelime tabanlı mock'tur.
- Hasar ek ücreti cüzdandan otomatik tahsil edilmez ve `TripFare`'e dahil edilmez.
- OTP/SMS ve ödeme mock'tur.
- Seed dışı (kayıt olan) kullanıcılar için otomatik cüzdan oluşturulması yoktur;
  cüzdan gerektiren işlemler seed kullanıcılarla veya ayrı bir cüzdan yaratımıyla
  yürütülmelidir.
- Uyarı listeleme/arıza listeleme gibi bazı okuma uçlarında sahiplik kontrolü
  modüle göre değişir; detaylar ilgili servislerdedir.

---

## 16. Sık Karşılaşılan Sorunlar

| Belirti | Olası neden / çözüm |
|---|---|
| PostGIS fonksiyon hatası | Veritabanında `postgis` eklentisi yok; `CREATE EXTENSION postgis` |
| 401 Unauthorized | Eksik/geçersiz `Bearer` token |
| 403 Forbidden | İşlem yapan kullanıcı seyahatin sahibi değil |
| 409 Conflict | Araç uygun değil / bakiye yetersiz / seyahat IN_PROGRESS değil |
| 400 Bad Request | Geçersiz enum/parametre (örn. yanlış status) veya koordinat/hedef |
| Login başarısız | Seed kullanıcı şifresi `123456789`; kayıt olan kullanıcı önce OTP doğrulamalı |

---


