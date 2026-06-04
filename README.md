# Finance Portal

## Proje Nedir?
Finance Portal, kullanıcıların finansal enstrümanlarını (hisse senedi, döviz, kripto, tahvil, kıymetli maden, fon, VİOP vadeli işlemler) takip edebildiği, portföylerini yönetebildiği, finans ile ilgili haberleri takip edebildiği, finansal enstrümanları karşılaştırabildiği ve fiyat alarmı kurebildiği full-stack bir web uygulamasıdır.

## Özellikler

* Finansal Enstrümanlar — Döviz, hisse, kripto, tahvil, kıymetli maden, fon, VİOP vadeli işlemler
* Veri Kaynakları — TCMB, Yahoo Finance, İş Yatırım, TCMB EVDS
* Portföy Yönetimi — Portföy oluşturma, alış/satış işlemleri, kar/zarar takibi
* Fiyat Alarmları — Hedef fiyat belirle, uygulama bildirimi al
* Takip Listesi — İlgilendiğin enstrümanları takibe al
* Tarihsel Fiyat Grafikleri — Mum ve alan grafiği ile geçmiş fiyat analizi
* Anlık Fiyat Güncellemeleri — WebSocket ile sayfayı yenilemeden fiyat takibi
* Haberler — Finansal haberler (Türkçe & İngilizce)
* Bildirim Sistemi — Uygulama içi bildirimler
* Çok Dil Desteği — Türkçe ve İngilizce arayüz, haber içerikleri otomatik çeviri
* Kullanıcı Profili — Profil görüntüleme, tema ayarı, bildirim tercihleri
* Email Doğrulama — Kayıt sonrası Mailhog üzerinden email doğrulama
* 2FA — TOTP ile zorunlu iki faktörlü kimlik doğrulama
* Admin Paneli — Fiyat güncelleme, kullanıcı yönetimi, haber yönetimi, portföy yönetimi
* Monitoring — Prometheus + Grafana ile JVM ve uygulama metrikleri
* Tracing — OpenTelemetry + Data Prepper ile distributed tracing
* Log Yönetimi — OpenSearch Dashboards ile merkezi log takibi
* Cache — Redis ile yüksek performanslı veri erişimi

## Teknoloji Stack

| Katman | Teknoloji |
|--------|-----------|
| Frontend | React.js + Tailwind CSS |
| i18n | react-i18next + i18next |
| Backend | Java 21 + Spring Boot 3.2.5 |
| Veritabanı | PostgreSQL 16 |
| Cache | Redis 7 |
| Auth | Keycloak 23.0 + OpenLDAP |
| LDAP UI | phpLDAPadmin |
| Mesajlaşma | Apache Kafka |
| Çeviri | LibreTranslate (TR ↔ EN) |
| Log Pipeline | Log4j2 → Kafka → Logstash → OpenSearch |
| Log UI | OpenSearch Dashboards |
| İzleme | Prometheus + Grafana |
| Tracing | OpenTelemetry + Data Prepper |
| Email | Mailhog |
| Migration | Flyway |
| Dokümantasyon | Swagger/OpenAPI |

## Gereksinimler

Kurulum öncesi aşağıdakilerin yüklü olması gerekir:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (minimum **8GB RAM** ayrılması önerilir)
- [Git](https://git-scm.com/)

> **Önemli:** Docker Desktop'ta RAM limitini artırmak için:
> Settings → Resources → Memory → **8GB veya üzeri** yapın.

## Kurulum

### 1. Repoyu klonla

```bash
git clone https://github.com/YukselMertKirdar67/finance-portal
cd finance-portal
```

### 2. Container'ları başlat

```bash
docker-compose up --build
```

> İlk başlatmada tüm image'ların indirilmesi, Keycloak'ın ayağa kalkması ve **LibreTranslate'in dil modellerini indirmesi** **10-15 dakika** sürebilir.

> **LibreTranslate Notu:** İlk başlatmada ~1-2 GB dil modeli indirilir. Bu işlem tamamlanmadan haber çevirileri çalışmaz ancak uygulama normal şekilde çalışmaya devam eder. Durumu kontrol etmek için:
> ```bash
> docker logs finance-libretranslate
> ```
> `Listening at: http://[::]:5000` satırını gördüğünüzde çeviri servisi hazırdır.

### 3. Keycloak Realm Import

Uygulama ilk kez başlatıldığında Keycloak realm'ini manuel import etmen gerekir:

1. [http://localhost:8180](http://localhost:8180) adresine git
2. **admin / admin** ile giriş yap
3. Sol üstten **Create Realm** butonuna tıkla
4. **Browse** → proje klasöründeki `keycloak-realm-export.json` dosyasını seç
5. **Create** butonuna tıkla

> Import başarılı olursa sol üstte **finance-portal** realm'i görünür.

## Servis URL'leri

| Servis | URL | Kullanıcı Adı | Şifre |
|--------|-----|---------------|-------|
| Frontend | http://localhost:3000 | — | — |
| Backend API | http://localhost:8080/api/v1 | — | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — | — |
| Keycloak | http://localhost:8180 | admin | admin |
| OpenSearch Dashboards | http://localhost:5601 | — | — |
| Prometheus | http://localhost:9090 | — | — |
| Grafana | http://localhost:3001 | admin | admin |
| Mailhog | http://localhost:8025 | — | — |
| phpLDAPadmin | http://localhost:8090 | cn=admin,dc=financeportal,dc=com | admin123 |
| LibreTranslate | http://localhost:5000 | — | — |

## Yeni Kullanıcı Oluşturma (Önerilen)

Varsayılan kullanıcılar 2FA ile tanımlandığından **yeni kullanıcı oluşturmanız önerilir:**

1. [http://localhost:3000](http://localhost:3000) adresine git
2. **Kayıt Ol** butonuna tıkla
3. Kullanıcı adı, email ve şifre gir
4. **Mailhog**'dan email doğrulaması yap:
   - [http://localhost:8025](http://localhost:8025) adresine git
   - Gelen doğrulama emailini aç ve linke tıkla
5. Email doğrulandıktan sonra **2FA kurulum** sayfasına yönlendirilirsin
6. **Google Authenticator** veya **Authy** uygulamasını telefonuna indir
7. Uygulama ile QR kodu tarat
8. Gösterilen 6 haneli kodu gir → 2FA kurulumu tamamlandı
9. Artık her girişte: **kullanıcı adı + şifre + 6 haneli kod** gerekir

## Varsayılan Kullanıcılar

Keycloak'ta iki varsayılan kullanıcı tanımlıdır:

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| adminuser | 123456 | ADMIN |
| Yuksel123 | 123456 | USER |

> **NOT:** Bu kullanıcılar 2FA (TOTP) ile tanımlanmıştır. Giriş yaparken Google Authenticator veya benzeri bir uygulama ile doğrulama kodu gerekir. Bu yüzden yeni bir kullanıcı oluşturarak giriş yapmanız önerilir.

## Çok Dil Desteği (i18n)

Uygulama **Türkçe** ve **İngilizce** dillerini desteklemektedir.

- **Dil değiştirme:** Header'daki 🌐 butonuna veya Landing Page'deki dil butonuna tıkla
- **Dil tercihi** tarayıcıda kalıcı olarak saklanır
- **Haber içerikleri** seçilen dile göre otomatik olarak çevrilir (LibreTranslate)
- Dil değiştirildiğinde haber sayfaları otomatik yenilenir

### Haber Çevirisi için Admin İşlemi

Haberlerin İngilizce çevirilerini oluşturmak için admin panelinden haber çekme işlemi yapılmalıdır:

1. Admin hesabıyla giriş yap
2. **Admin Panel → Haber Yönetimi** sayfasına git
3. **Haber Çek** butonuna tıkla
4. Haberler çekilirken LibreTranslate otomatik olarak İngilizce çeviriyi oluşturur

## Monitoring

### Grafana

1. [http://localhost:3001](http://localhost:3001) adresine git (admin/admin)
2. Sol menüden **Dashboards** → **JVM (Micrometer)** dashboard'unu aç
3. **Application** dropdown → `finance-backend` seç
4. **Instance** dropdown → `finance-backend:8080` seç

### Prometheus

1. [http://localhost:9090](http://localhost:9090) adresine git
2. **Status → Targets** sayfasında `finance-backend` → **UP** görünmeli

### OpenSearch Logs

1. [http://localhost:5601](http://localhost:5601) adresine git
2. **Discover** sekmesinden uygulama loglarını görüntüle

### Distributed Tracing (OpenTelemetry)

1. [http://localhost:5601](http://localhost:5601) adresine git
2. **Observability → Traces** sekmesinden request trace'lerini görüntüle

## Proje Yapısı

```
finance-portal/
├── docker-compose.yml
├── prometheus.yml
├── logstash.conf
├── otel-collector-config.yml
├── data-prepper-config.yml
├── keycloak-realm-export.json
├── README.md
├── grafana/
│   └── provisioning/
├── backend/
│   └── finance-portal-backend/
│       ├── src/
│       │   ├── main/java/com/financeportal/backend/
│       │   │   ├── Config/
│       │   │   │   └── MessageConfig.java
│       │   │   ├── Translation/
│       │   │   │   └── TranslationService.java
│       │   │   ├── Instrument/
│       │   │   ├── Portfolio/
│       │   │   ├── Watchlist/
│       │   │   ├── News/
│       │   │   ├── PriceAlert/
│       │   │   ├── Notification/
│       │   │   ├── User/
│       │   │   ├── WebSocket/
│       │   │   └── ...
│       │   └── resources/
│       │       ├── messages_tr.properties
│       │       ├── messages_en.properties
│       │       └── db/migration/
│       └── Dockerfile
└── frontend/
    └── finance-portal-frontend/
        ├── src/
        │   ├── API/
        │   ├── Components/
        │   ├── Hooks/
        │   ├── context/
        │   ├── locales/
        │   │   ├── tr.json
        │   │   └── en.json
        │   └── i18n.js
        └── Dockerfile
```

## Sık Karşılaşılan Sorunlar

**Backend başlamıyor:**
```bash
docker-compose logs backend
```
Flyway migration hatası varsa:
```bash
docker exec -it finance-postgres psql -U postgres -d FinancialWebProgram -c "DELETE FROM flyway_schema_history WHERE success = false;"
docker-compose restart backend
```

**Keycloak bağlantı hatası:**
- Keycloak'ın tamamen ayağa kalkması 2-3 dakika sürebilir
- `docker-compose ps` ile `finance-keycloak` → `healthy` olmasını bekle

**LibreTranslate başlamıyor / çeviri çalışmıyor:**
```bash
docker logs finance-libretranslate
```
- İlk başlatmada dil modelleri indirilir, bu 5-10 dakika sürebilir
- `Listening at: http://[::]:5000` satırını görene kadar bekle
- Çeviri servisi hazır değilken haberler orijinal dilde gösterilir (fallback)

**Port çakışması:**
- 3000, 8080, 8180, 5000, 5432, 6379 portlarının boş olduğundan emin ol

## CI/CD Pipeline (Tasarım)

Bu proje için önerilen CI/CD pipeline aşağıdaki gibi tasarlanmıştır:

```yaml
# .github/workflows/ci-cd.yml
name: Finance Portal CI/CD

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Build Backend
        run: |
          cd backend/finance-portal-backend
          mvn clean package -DskipTests

      - name: Build Docker Images
        run: docker-compose build

      - name: Deploy
        run: docker-compose up -d
```

## API Dokümantasyonu

Uygulama çalışırken Swagger UI üzerinden tüm endpoint'leri görüntüleyebilirsin:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Tüm endpoint'ler `/api/v1/` prefix'i ile başlar.
